# Onboarding Functions

Repository that contains Azure functions designed for onboarding asynchronous flow activities.
These functions handle all asynchronous activities related to preparing and completing the onboarding process. Indeed, they are activated by the onboarding microservice upon receiving an onboarding request.

1. StartOnboardingOrchestration:

It is triggered by http request at GET or POST `/api/StartOnboardingOrchestration?onboardingId={onboardingId}` where onboardingId is a reference to onboarding which you want to process.

### Contract Signature

You can enable the signature inside contracts when there are builded setting PAGOPA_SIGNATURE_SOURCE env (default value is `disabled`) as `local` if you want to use Pkcs7HashSignService or `aruba` for ArubaPkcs7HashSignService. Look at this [README](../../libs/onboarding-sdk-crypto/README.md) for more informations.


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

### Blob storage authentication

The application supports both connection-string and Managed Identity authentication for product and contract/document blob storage.
When `BLOB_STORAGE_CONN_STRING_CONTRACT` or `BLOB_STORAGE_CONN_STRING_PRODUCT` is set, the connection string is used.
When the connection string is empty, configure the corresponding storage account name and managed identity client id:

| **Storage** | **Account Environment Variable** | **Managed Identity Client ID Environment Variable** |
|-------------|----------------------------------|-----------------------------------------------------|
| Contract/documents | BLOB_STORAGE_ACCOUNT_NAME_CONTRACT | BLOB_STORAGE_MANAGED_IDENTITY_CLIENT_ID_CONTRACT |
| Product | BLOB_STORAGE_ACCOUNT_NAME_PRODUCT | BLOB_STORAGE_MANAGED_IDENTITY_CLIENT_ID_PRODUCT |

Before enabling this in Azure, the Function App must have the referenced user-assigned managed identities attached by infrastructure.
Local and Cucumber integration tests must set connection strings explicitly for Azurite; Azure environments must leave `BLOB_STORAGE_CONN_STRING_CONTRACT` and `BLOB_STORAGE_CONN_STRING_PRODUCT` unset. The main `application.properties` does not declare connection-string properties, while test properties provide them explicitly.


### Custom settings

Set `STANDARD_NOTIFICATION_RELATED_DOCUMENTS_ENABLED=true` to add the optional flat `relatedDocuments` array to the standard `SC-Contracts` topic. The default is `false`, so the existing queue payload remains unchanged until the consumer is ready. SAP and webhook payloads are never enriched by this flag.

Each item contains its document identifier, logical and physical names, lower-case document type, MIME type, creation timestamp and full storage path. The array is a full snapshot of ATTACHMENT and USER documents linked to the root onboarding; if none exists, it is empty.

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

## Cucumber integration tests in IntelliJ

The Cucumber suite exercises the Functions Host running in the Docker integration stack; it does not use the development configuration or credentials of an Azure environment.

### Prerequisites

- Docker must be running and able to pull the images used by `src/test/resources/docker-compose.yml` (access to `ghcr.io/pagopa` may be required).
- Maven dependencies must be available. If required by the Docker build, configure the local Maven settings in `~/.m2/settings.xml`.

Start the stack from the repository root:

```shell
docker compose -f apps/onboarding-functions/src/test/resources/docker-compose.yml up --build
```

The stack starts MongoDB, Azurite, MockServer, User MS, Institution MS, Document MS and the Function Host. The Function endpoint used by the feature is `http://localhost:8090`.

> **Test data only:** all environment variables, keys, tokens, connection strings, fixtures and databases used by this stack are fake/local test data. MongoDB and Azurite run in Docker and are not connected to Azure or to a real database.

### IntelliJ run configuration

Create a **Cucumber Java** configuration with these values (the shared configuration is [Feature_ onboarding-fn.run.xml](../../.run/Feature_%20onboarding-fn.run.xml)):

| Field | Value |
| --- | --- |
| Feature file | `apps/onboarding-functions/src/test/resources/features/onboarding-fn.feature` |
| Main class | `it.pagopa.selfcare.onboarding.steps.OnboardingFunctionStep` |
| Module | `onboarding-functions` |
| Working directory | module directory (`apps/onboarding-functions`) |
| Program arguments | `--plugin teamcity` (optional) |

Run it only after the `onboarding-function` container is ready. The test profile reads the local test properties, fixtures and certificates already versioned under `src/test/resources`; do not replace them with application credentials, connection strings, API keys, or environment-specific values.

Stop and remove the stack when finished:

```shell
docker compose -f apps/onboarding-functions/src/test/resources/docker-compose.yml down
```

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
