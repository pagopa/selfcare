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

## Integration tests
A new suite of integration tests written with cucumber was added in the `it.pagopa.selfcare.user_group.integration_tests` package.
The tests are currently disabled by default, to run the tests locally:

1. Start a local mongodb at port 27017, you can use the docker-compose.yml in this folder and run:
    ```shell script
    docker-compose up mongodb
    ```

2. Run the user-group-ms locally at port 8082 (by setting the environment variable MS_USER_GROUP_SERVER_PORT) and using the test public key in `src/test/resources/key/public-key.pub`
   (by setting the value in a single line of the env var JWT_TOKEN_PUBLIC_KEY).

3. Comment the line starting with @ExcludeTags inside the CucumberSuite file and run the test with maven:
    ```shell script
   mvn test -Dtest=CucumberSuite
   ```
