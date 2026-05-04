# Microservice dashboard-backend

Microservice responsible for orchestrating the functionalities required for user dashboard activities. It retrieves institutions for a specific user by identifying their roles and permissions. It manages delegations related to an institution and technology partner and allows access to individual product back-office.

## Prerequisites

    Java version: 17
    Maven version: 3.9.*

## Required Configuration Properties

Before running you must set these properties as environment variables.


| **Property**                                             | **Environment Variable**                 | **Default** | **Required** |
|----------------------------------------------------------|------------------------------------------|-------------|:------------:|
| rest-client.user-registry.base-url<br/>                  | USERVICE_USER_REGISTRY_URL               |             |     yes      |
| feign.client.config.user-registry.*.x-api-key<br/>       | USERVICE_USER_REGISTRY_API_KEY           |             |     yes      |
| rest-client.pago-pa-backoffice.base-url<br/>             | PAGO_PA_BACKOFFICE_URL                   |             |     yes      |
| backoffice.pago-pa.subscriptionKey<br/>                  | BACKOFFICE_PAGO_PA_API_KEY               |             |     yes      |
| jwt.signingKey<br/> <br/>                                | JWT_TOKEN_PUBLIC_KEY                     |             |     yes      |

## Setup 

### GitHub Credentials for selfcare-onboarding-sdk

To use the selfcare-onboarding-sdk, you need to configure your Maven settings to include GitHub credentials. This allows Maven to authenticate and download the required dependencies.

1. Open or create the ~/.m2/settings.xml file on your local machine.
2. Add the following <server> configuration to the <servers> section:

```xml script
<servers>
    <server>
        <id>selfcare-onboarding</id>
        <username>**github_username**</username>
        <password>**ghp_token**</password>
    </server>
</servers>

```

## Running the application

You can run your application using:
```shell script
./mvnw install
./mvnw spring-boot:run -pl app
```

Application will respond to the url http://localhost:8080

## Cucumber Tests (Integration Tests)
A new suite of integration tests written with cucumber was added in `it.pagopa.selfcare.dashboard.integration_test` package.

To run the Cucumber tests locally, execute it.pagopa.selfcare.dashboard.integration_testCucumberSuite

To run a single test or a specific feature file, open the file and press the play button for the corresponding test (or the file). 
