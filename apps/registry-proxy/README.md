# selfcare-ms-party-registry-proxy

## Description
This Spring Boot-based microservice is designed to handle several key functionalities in onboarding operations domain. It includes business logic for proxy data sources used in SelfCare, such as:

- IPA data for PA, AOO and UO
- ANAC
- IVASS 
- INFOCAMERE
- PDND_INFOCAMERE
- ADE

## Prerequisites
Before running the microservice, ensure you have installed:

- Java JDK 17 or higher
- Maven 3.6 or higher
- Connection to VPN selc-d-vnet

## Configuration
Look at app/src/main/resources/`application.yml` file to set up environment-specific settings, such as database details.

### Multitenancy (Step 2)

`registry-proxy` uses `selc-commons-tenant` and the shared Spring JWT security,
as in `user-group-ms`. Every authenticated request reconciles `X-Tenant-Id`
with the verified JWT `tenant_id`, selects the verification key from the
canonical `SELFCARE_TENANT_DATA_ISOLATION` registry, and clears `TenantContext`
after completion. Missing, duplicated, unknown or mismatched tenant identities
are rejected. Resource selection never reads request headers directly.

There is no Mongo database in this service. `tenant.mongodb.required=false`
permits JWT/storage-only registry entries without configuring a dummy Mongo
connection; Mongo remains mandatory by default for existing stateful consumers.

| Resource | Scope |
| --- | --- |
| IPA, ANAC, IVASS, geographic taxonomies and their public indexes/files | Explicitly global, environment-specific configuration; scheduled refreshes do not require a request tenant |
| Onboarding search | Tenant-owned; `registryProxy.search` selects endpoint, dedicated index and credential reference per operation |
| National Registries | `registryProxy.nationalRegistries` selects endpoint and API key per tenant |
| PDND InfoCamere and Invitalia | Tenant-specific credentials, with product-based Invitalia selection within that tenant |
| PDND result, assertion and token caches | Tenant-qualified; credential profile/identity is also retained in cache keys |
| Institution service calls | Propagate the authenticated bearer token and validated tenant; no static functions-token fallback |
| Optional visura archival | `storages.visure`, resolved from the persisted tenant supplied to the async operation, never from worker thread state |

Each registry entry requires `jwt.publicKeyEnvVar` and a `registryProxy` object.
Example entry (all credentials below are environment-variable **names**, not values):

```json
{
  "AR": {
    "jwt": {
      "publicKeyEnvVar": "JWT_PUBLIC_KEY_AR",
      "issuers": ["SPID", "PAGOPA"],
      "audiences": ["api.dev.selfcare.pagopa.it"]
    },
    "registryProxy": {
      "search": {
        "endpoint": "https://search-ar.search.windows.net",
        "onboardingIndex": "onboarding-index-ar",
        "apiKeyEnvVar": "SEARCH_KEY_AR"
      },
      "nationalRegistries": {
        "endpoint": "https://national-registries.example",
        "apiKeyEnvVar": "NATIONAL_REGISTRY_API_KEY_AR"
      },
      "pdnd": {
        "clientIdEnvVar": "PDND_INFOCAMERE_CLIENT_ID_AR",
        "privateKeyEnvVar": "PDND_INFOCAMERE_PRIVATE_KEY_AR",
        "kidEnvVar": "PDND_INFOCAMERE_KID_AR",
        "purposeIdEnvVar": "PDND_INFOCAMERE_PURPOSE_ID_AR",
        "audience": "auth.interop.pagopa.it/client-assertion"
      },
      "invitaliaProducts": []
    }
  }
}
```

Add a separate PNPG entry and distinct secret references to serve both tenants.
`pdndInvitalia` has the same schema as `pdnd`; `invitaliaProducts` selects that
profile and requires it to be configured. Optional omitted resources fail on
use, never fall back to another tenant or legacy flat properties. Configured
resources and credential references are validated at startup. Onboarding indexes
must be distinct even when the Search account is shared. External endpoints must
be trusted HTTPS configuration, not request parameters.

Each of the six Terraform deployments defines `SELFCARE_TENANT_DATA_ISOLATION`
directly in its `infra/resources/registry-proxy/<env>-<tenant>/registry-proxy.tf`,
not in the shared `local-env` module. Existing Key Vault secret names are preserved
with tenant-qualified application configuration keys. No state transfer, traffic
cutover or resource deletion is performed. PNPG onboarding search is intentionally
unmapped until a dedicated index and credentials are provisioned. The unused archival helper now
returns a `CompletableFuture` and requires an explicit `tenantId`; any future
caller must retain that identity in its durable payload and observe delivery
failures. Configure `storages.visure` with a tenant-specific container/prefix and
grants before enabling archival; the legacy Dapr state component is not a fallback.

Before consolidating deployments:

- Publish Spring commons `2.23.0` before releasing this application.
- Set `TENANT_SUPPORTED_TENANTS=AR,PNPG`, supply both registry entries and
  secret-backed variables, and verify access to both tenants' downstream resources.
- Set `JWT_TENANT_CLAIM_REQUIRED=true` after both issuers and machine clients emit
  `tenant_id`. A multi-tenant deployment refuses to start without it. Legacy
  Kubernetes tokens without a tenant binding are rejected in this mode.
- Configure each tenant's JWT `issuers` and `audiences` consistently with the
  trusted issuer contracts (both are mandatory for shared deployments); configured
  routes enforce RS256 and expiration. The optional global `JWT_EXPECTED_ISSUER` /
  `JWT_EXPECTED_AUDIENCE` settings impose additional restrictions. The Terraform
  tenant entries also enforce these checks in the existing single-tenant stacks:
  verify issuer/audience contracts and token expiration before releasing them.
  Migrate dependencies first and verify both tenant routes before enabling unified
  APIM ingress.

Request audit logs contain route patterns, validated tenant and outcome, not
payloads, query values or authorization credentials. Public-registry clients do
not forward user JWTs or tenant headers to external providers.

## Installation and Local Startup
To run the microservice locally, follow these steps:

1. **Clone the Repository**

```shell script
git clone https://github.com/pagopa/selfcare-ms-party-registry-proxy.git
cd selfcare-ms-party-registry-proxy
```

2. **Build the Project**

```shell script
mvn clean install
```

2. **Start the Application**

```shell script
mvn spring-boot:run -pl app
```

## Usage
After starting, the microservice will be available at `http://localhost:8080/`.

To use the API, refer to the Swagger UI documentation (if available) at `http://localhost:8080/swagger-ui.html`.

## DAPR
### Configure
Before run DAPR add:
.dapr/components/eventhub-pubsub.yaml
```shell script
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: eventhub-pubsub
spec:
  type: pubsub.azure.eventhubs
  version: v1
  metadata:
    - name: brokers
      value: "selc-ENV-eventhub-ns.servicebus.windows.net:9093"
    - name: authType
      value: "password"
    - name: saslUsername
      value: "$ConnectionString"
    - name: saslPassword
      value: "Endpoint=sb://selc-ENV-eventhub-ns.servicebus.windows.net/;SharedAccessKeyName=selc-proxy;SharedAccessKey=SHARED_ACCESS_KEY=;EntityPath=sc-contracts"
    - name: consumerGroup
      value: "party-proxy"
```


.dapr/config/config.yaml
```shell script
apiVersion: dapr.io/v1alpha1
kind: Configuration
metadata:
  name: dapr-config
  namespace: default
spec:
  tracing:
    samplingRate: "1"
    zipkin:
      endpointAddress: "http://localhost:9411/api/v2/spans"
  logging:
    apiLogging:
      enabled: true
  httpPipeline:
    handlers:
      - name: cors
        type: middleware.http.cors
        spec:
          allowedOrigins: ["*"]
          allowedMethods: ["GET", "POST", "PUT", "DELETE"]
          allowedHeaders: ["*"]
```

### Run

```shell script
dapr init
dapr run --app-id dapr-consumer --app-port 8080 --dapr-http-port 3500 --components-path ./.dapr/components
```

### Logs
```shell script
az containerapp logs show --name ca-dapr-consumer-dev --resource-group rg-dapr-consumer --container daprd
```
