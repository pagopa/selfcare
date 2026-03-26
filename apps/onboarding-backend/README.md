# Progetto Selfcare Onboarding Backend 
An orchestrator for the onboarding process

## Configuration Properties

#### Application properties

| **Property** | **Enviroment Variable** | **Default** | **Required** |
|--------------|-------------------------|-------------|:------------:|
|server.port|B4F_ONBOARDING_SERVER_PORT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/app/src/main/resources/config/application.yml)| yes |
|logging.level.it.pagopa.selfcare| B4F_ONBOARDING_LOG_LEVEL |<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/app/src/main/resources/config/application.yml)| yes |


#### REST client Configurations

| **Property** | **Enviroment Variable** | **Default** | **Required** |
|--------------|-------------------------|-------------|:------------:|
|rest-client.party-process.base-url|USERVICE_PARTY_PROCESS_URL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/party-process-rest-client.properties)| yes |
|feign.client.config.party-process.connectTimeout|USERVICE_PARTY_PROCESS_REST_CLIENT_CONNECT_TIMEOUT<br>REST_CLIENT_CONNECT_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/party-process-rest-client.properties)| yes |
|feign.client.config.party-process.readTimeout|USERVICE_PARTY_PROCESS_REST_CLIENT_READ_TIMEOUT<br>REST_CLIENT_READ_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/party-process-rest-client.properties)| yes |
|feign.client.config.party-process.loggerLevel|USERVICE_PARTY_PROCESS_REST_CLIENT_LOGGER_LEVEL<br>REST_CLIENT_LOGGER_LEVEL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/party-process-rest-client.properties)| yes |
|rest-client.products.base-url|MS_PRODUCT_URL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/products-rest-client.properties)| yes |
|feign.client.config.products.connectTimeout|MS_PRODUCT_REST_CLIENT_CONNECT_TIMEOUT<br>REST_CLIENT_CONNECT_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/products-rest-client.properties)| yes |
|feign.client.config.products.readTimeout|MS_PRODUCT_REST_CLIENT_READ_TIMEOUT<br>REST_CLIENT_READ_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/products-rest-client.properties)| yes |
|feign.client.config.products.loggerLevel|MS_PRODUCT_REST_CLIENT_LOGGER_LEVEL<br>REST_CLIENT_LOGGER_LEVEL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/products-rest-client.properties)| yes |
|rest-client.user-registry.base-url|USERVICE_USER_REGISTRY_URL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |
|feign.client.config.user-registry.defaultRequestHeaders.x-api-key|USERVICE_USERVICE_USER_REGISTRY_API_KEY|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |
|feign.client.config.user-registry.connectTimeout|USERVICE_USER_REGISTRY_REST_CLIENT_CONNECT_TIMEOUT<br>REST_CLIENT_CONNECT_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |
|feign.client.config.user-registry.readTimeout|USERVICE_USER_REGISTRY_REST_CLIENT_READ_TIMEOUT<br>REST_CLIENT_READ_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |
|feign.client.config.user-registry.loggerLevel|USERVICE_USER_REGISTRY_REST_CLIENT_LOGGER_LEVEL<br>REST_CLIENT_LOGGER_LEVEL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-backend/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |


#### Core Configurations

| **Property** | **Enviroment Variable** | **Default** | **Required** |
|--------------|-------------------------|-------------|:------------:|
