# Progetto Selfcare Onboarding Backend 
An orchestrator for the onboarding process

## Configuration Properties

#### Application properties

| **Property** | **Enviroment Variable** | **Default** | **Required** |
|--------------|-------------------------|-------------|:------------:|
|quarkus.http.port|B4F_ONBOARDING_SERVER_PORT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|quarkus.log.category.it.pagopa.selfcare.level| B4F_ONBOARDING_LOG_LEVEL |<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|quarkus.log.level| QUARKUS_LOG_LEVEL |[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |



#### REST client Configurations

| **Property** | **Enviroment Variable** | **Default** | **Required** |
|--------------|-------------------------|-------------|:------------:|
|rest-client.ms-onboarding.base-url|MS_ONBOARDING_URL|[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|rest-client.ms-user.base-url|MS_USER_URL|[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|rest-client.ms-product.base-url|MS_PRODUCT_URL|[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|rest-client.ms-core.base-url|MS_CORE_URL|[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|rest-client.party-process.base-url|USERVICE_PARTY_PROCESS_URL|[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|rest-client.party-registry-proxy.base-url|USERVICE_PARTY_REGISTRY_PROXY_URL|[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|rest-client.user-registry.base-url|USERVICE_USER_REGISTRY_URL|[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|quarkus.openapi-generator.user_registry_json.auth.api_key.api-key|USER-REGISTRY-API-KEY|[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |
|quarkus.openapi-generator.onboarding_functions_json.auth.api_key.api-key|ONBOARDING-FUNCTIONS-API-KEY|[application.properties](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/src/main/resources/application.properties)| yes |


#### Core Configurations

| **Property** | **Enviroment Variable** | **Default** | **Required** |
|--------------|-------------------------|-------------|:------------:|
