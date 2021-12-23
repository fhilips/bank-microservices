
package com.bank.acount.controller;



import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bank.acount.config.AccountsServiceConfig;
import com.bank.acount.model.Accounts;
import com.bank.acount.model.Cards;
import com.bank.acount.model.Customer;
import com.bank.acount.model.CustomerDetails;
import com.bank.acount.model.Loans;
import com.bank.acount.model.Properties;
import com.bank.acount.repository.AccountsRepository;
import com.bank.acount.service.client.CardsFeignClient;
import com.bank.acount.service.client.LoansFeignClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.github.resilience4j.retry.annotation.Retry;


@RestController
public class AccountsController {
	
	@Autowired
	private AccountsRepository accountsRepository;
	
	@Autowired
	AccountsServiceConfig accountsConfig;	
	
	@Autowired
	LoansFeignClient loansFeignClient;

	@Autowired
	CardsFeignClient cardsFeignClient;

	@PostMapping("/myAccount")
	public Accounts getAccountDetails(@RequestBody Customer customer) {

		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		if (accounts != null) {
			return accounts;
		} else {
			return null;
		}
	}	
	
	@GetMapping("/account/properties")
	public String getPropertyDetails() throws JsonProcessingException {
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		Properties properties = 
				new Properties(accountsConfig.getMsg(), accountsConfig.getBuildVersion(),
				accountsConfig.getMailDetails(), accountsConfig.getActiveBranches());
		String jsonStr = ow.writeValueAsString(properties);
		return jsonStr;
	}
	
	@PostMapping("/myCustomerDetails")
//	@CircuitBreaker(name = "detailsForCustomerSupportApp", fallbackMethod = "myCustomerDetailsFallBack")
	@Retry(name = "retryForCustomerDetails", fallbackMethod = "myCustomerDetailsFallBack")
	public CustomerDetails myCustomerDetails(@RequestBody Customer customer) {
		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		List<Loans> loans = loansFeignClient.getLoansDetails(customer);
		List<Cards> cards = cardsFeignClient.getCardDetails(customer);

		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		customerDetails.setLoans(loans);
		customerDetails.setCards(cards);
		
		return customerDetails;

	}
	private CustomerDetails myCustomerDetailsFallBack(Customer customer, Throwable t) {
		Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId());
		List<Loans> loans = loansFeignClient.getLoansDetails(customer);
		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setAccounts(accounts);
		customerDetails.setLoans(loans);
		return customerDetails;
	}

}
