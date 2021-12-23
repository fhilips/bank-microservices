
FROM openjdk:11-slim as build

COPY target/account-0.0.1-SNAPSHOT.jar account-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java","-jar","/account-0.0.1-SNAPSHOT.jar"]