# Onboarding Functions

Repository that contains Azure functions designed for onboarding asynchronous flow activities.
These functions handle all asynchronous activities related to preparing and completing the onboarding process. Indeed, they are activated by the onboarding microservice upon receiving an onboarding request.

1. StartOnboardingOrchestration:

It is triggered by http request at GET or POST `/api/StartOnboardingOrchestration?onboardingId={onboardingId}` where onboardingId is a reference to onboarding which you want to process.

### Contract Signature

You can enable the signature inside contracts when there are builded setting PAGOPA_SIGNATURE_SOURCE env (default value is `disabled`) as `local` if you want to use Pkcs7HashSignService or `aruba` for ArubaPkcs7HashSignService. Look at this [README](https://github.com/pagopa/selfcare-onboarding/tree/develop/libs/onboarding-sdk-crypto#readme) for more informations.


## Running locally


### Install the Azure Functions Core Tools

Follow this [guide](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=macos%2Cisolated-process%2Cnode-v4%2Cpython-v2%2Chttp-trigger%2Ccontainer-apps&pivots=programming-language-java) for recommended way to install Core Tools on the operating system of your local development computer.

### Configuration Properties

Before running you must set these properties as environment variables.


| **Property**                                                           | **Environment Variable**   | **Default** | **Required** |
|------------------------------------------------------------------------|----------------------------|-------------|:------------:|
| quarkus.mongodb.connection-string<br/>                                 | MONGODB_CONNECTION_URI     |             |     yes      |
| quarkus.openapi-generator.user_registry_json.auth.api_key.api-key<br/> | USER_REGISTRY_API_KEY      |             |     yes      |
| quarkus.rest-client."*.user_registry_json.api.UserApi".url<br/>        | USER_REGISTRY_URL          |             |     yes      |

### Local settings
Under the path "apps/onboarding-functions" you should check the presence of "local.settings.json".<br>
If it's not present than you should add a file named "local.settings.json", containing the following json:<br>
```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "FUNCTIONS_WORKER_RUNTIME": "java"
  }
}
```

### Storage emulator: Azurite

Use the Azurite emulator for local Azure Storage development. Once installed, you must create `selc-d-contracts-blob` and `selc-d-product` container. Inside last one you have to put products.json file.

([guide](https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite?tabs=visual-studio))

### Install dependencies

At project root you must install dependencies:

```shell script
./mvnw install
```

### Packaging

The application can be packaged using:
```shell script
./mvnw package
```

It produces the `onboarding-functions-1.0.0-SNAPSHOT.jar` file in the `target/` directory.

### Start application

```shell script
./mvnw package quarkus:run
```

If you want enable debugging you must add -DenableDebug

```shell script
./mvnw quarkus:run -DenableDebug
```
You can follow this guide for debugging application in IntelliJ https://www.jetbrains.com/help/idea/tutorial-remote-debug.html

## Deploy

### Configuration Properties

Before deploy you must set these properties as environment variables.


| **Property**                                       | **Environment Variable**     | **Default** | **Required** |
|----------------------------------------------------|------------------------------|-------------|:------------:|
| quarkus.azure-functions.app-name<br/>              | AZURE_APP_NAME               |             |      no      |
| quarkus.azure-functions.subscription-id<br/>       | AZURE_SUBSCRIPTION_ID        |             |      no      |
| quarkus.azure-functions.resource-group<br/>        | AZURE_RESOURCE_GROUP         |             |      no      |
| quarkus.azure-functions.app-insights-key<br/>      | AZURE_APP_INSIGHTS_KEY       |             |      no      |
| quarkus.azure-functions.app-service-plan-name<br/> | AZURE_APP_SERVICE_PLAN_NAME  |             |      no      |


## Related Guides

- Azure Functions ([guide](https://quarkus.io/guides/azure-functions)): Write Microsoft Azure functions


