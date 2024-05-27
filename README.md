# selfcare-ms-user-group
microservice to manage group of users 

## Description
This Spring Boot-based microservice is designed to handle several key functionalities in the selfcare user-group operations domain and business logic for CRUD groups.

## Prerequisites
Before running the microservice, ensure you have installed:

- Java JDK 17 or higher
- Maven 3.6 or higher
- Connection to VPN selc-d-vnet

## Configuration
Look at app/src/main/resources/`application.yml` file to set up environment-specific settings, such as database details.

## Installation and Local Startup
To run the microservice locally, follow these steps:

1. **Build the Project**

```shell script
mvn clean install
```

2. **Start the Application**

```shell script
mvn spring-boot:run -pl app
```

Remember to set environment-specific settings (look above).

## Usage
After starting, the microservice will be available at `http://localhost:8080/`.

To use the API, refer to the Swagger UI documentation (if available) at `http://localhost:8080/swagger-ui.html`.


