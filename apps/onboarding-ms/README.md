# Microservice Onboarding

Repository that contains backend services synch for selfcare onboarding.

It implements CRUD operations for the 'onboarding' object and the business logic for the onboarding phase. During the onboarding process, the following activities are executed:

1. Check for the presence of users associated with the onboarding and potentially add them to the point of sale (pdv).
2. Validate the requested product's suitability and verify eligible roles.
3. Verify if there is an existing onboarding record for that institution and product.

After the data is saved, it invokes the function implemented by onboarding-functions to trigger asynchronous onboarding activities. 

### Disable starting async onboarding workflow

````properties
env ONBOARDING_ORCHESTRATION_ENABLED=false.
````

## Configuration Properties

Before running you must set these properties as environment variables.


| **Property**                                           | **Environment Variable**                 | **Default** | **Required** |
|--------------------------------------------------------|------------------------------------------|-------------|:------------:|
| tenant.registry.json<br/>                              | TENANT_REGISTRY_JSON                     |             |     yes      |
| Mongo AR connection string<br/>                        | MONGODB_CONNECTION_STRING_AR             |             |     yes      |
| Mongo PNPG connection string<br/>                      | MONGODB_CONNECTION_STRING_PNPG           |             |     yes      |
| mp.jwt.verify.publickey<br/>                           | JWT-PUBLIC-KEY                           |             |     yes      |
| quarkus.rest-client."**.UserApi".api-key<br/>          | USER-REGISTRY-API-KEY                    |             |     yes      |
| quarkus.rest-client."**.UserApi".url<br/>              | USER_REGISTRY_URL                        |             |     yes      |
| quarkus.rest-client."**.CoreApi".url<br/>              | MS_CORE_URL                              |             |     yes      |
| quarkus.rest-client."**.AooApi".url<br/>               | MS_PARTY_REGISTRY_URL                    |             |     yes      |
| quarkus.rest-client."**.UoApi".url<br/>                | MS_PARTY_REGISTRY_URL                    |             |     yes      |
| quarkus.rest-client."**.OrchestrationApi".url<br/>     | ONBOARDING_FUNCTIONS_URL                 |             |     yes      |
| quarkus.rest-client."**.OrchestrationApi".api-key<br/> | ONBOARDING-FUNCTIONS-API-KEY             |             |     yes      |
| quarkus.rest-client."**.InstitutionApi".url<br/>       | MS_USER_URL                              |             |     yes      |
| quarkus.rest-client."**.ProductApi".url<br/>           | MS_PRODUCT_URL                           | localhost:8080 | yes in deployments |
| tenant.supported-tenants<br/>                         | TENANT_SUPPORTED_TENANTS                 | AR,PNPG     | per deployment |
| tenant.storage.mandatory-keys<br/>                     | TENANT_STORAGE_MANDATORY_KEYS            | products    | yes with tenant SDK 0.2.0 |
| onboarding-ms.required-documents.enabled<br/>            | ONBOARDING-REQUIRED-DOCUMENTS-ENABLED    | false       |     no       |

> **_NOTE:_**  properties that contains secret must have the same name of its secret as uppercase.

### Product catalog and tenants

The catalog is read through Product MS, not directly from `products.json` on Azure Blob.
The generated client uses the current tenant-aware contract, such as
`/product/{tenantId}/{productId}/valid`. Keep `src/main/openapi/product.json`
aligned with `apps/product/src/main/docs/openapi.json`, then regenerate with the
existing Maven build; do not edit generated Java sources.

The resolved tenant is validated against the registry and used consistently in
the API path and `X-Tenant-Id`. Authorization is forwarded unchanged. An explicit
tenant must match an already initialized request context; a missing or conflicting
tenant must not cause a fallback to another tenant or to Blob.

Product lookup logs escape identifiers with the existing OWASP Java Encoder so
CR/LF and Unicode line separators cannot create forged log lines. Encoding applies
only to log arguments; Product API inputs and tenant validation remain unchanged.

Role mappings use the requested institution type, then the global/`DEFAULT`
mapping. Mappings for other institution types are not a fallback. Existing contract
imports and signed uploads retain their optional template metadata.
Building a signed-contract request requires a Product response, checked with
`Objects.requireNonNull`; a present product may still have no template metadata.

Onboarding does not read the catalog from Blob, but tenant SDK **0.2.0** still
requires `tenant.storage.mandatory-keys` and validates the corresponding
`storages.products` bindings. Keep the existing storage configuration and identity
wiring until the SDK update is released and adopted. This compatibility
configuration does not restore a Blob reader or Blob readiness check.
Product MS contract-template storage and Product CDC exports remain independent
and must not be removed.

### Catalog migration rollout

Before releasing onboarding, verify the deployed Product MS supports the current
tenant paths and its Mongo catalog contains the correct `tenantId` for every
enabled tenant. Repository configuration alone does not establish either fact.
Keep the tenant allowlist and Mongo/JWT configuration specific to each deployment.
The tenant SDK update is a separate release: this consumer remains on **0.2.0**.
Keep `tenant.storage.mandatory-keys=products` and valid bindings for every enabled
tenant in application, test and deployment configuration. The default local
registry references `BLOB_STORAGE_AR_PRODUCT_CONNECTION_STRING` and
`BLOB_STORAGE_PNPG_PRODUCT_CONNECTION_STRING`; the deployment registries retain
their existing managed identity configuration. Tests use emulator configuration,
not cloud credentials.

Remove these compatibility settings only after adopting an SDK that supports
absent mandatory-storage configuration. `--also-make` builds a local SDK only
when its version matches the dependency; it does not replace 0.2.0 with a newer
checkout version. No SDK publication is performed by the integration workflow.
Review the later cleanup plan for all six stacks; shared storage, identities and
role assignments must not be destroyed. The Product API migration must not depend
on applying that cleanup first.


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

For some endpoints 

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8083/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Tests

Run the module tests from the repository root with:

```shell
mvn -pl apps/onboarding-ms test
```

When available, prefer the workspace's resolved Nx targets (`pnpm nx show projects`
and `pnpm nx show project <project> --json`) over guessing a target name.

The catalog checks include `ProductOpenApiContractTest`, `ProductConfigUtilsTest`,
`ProductServiceImplTest` and `ProductServiceHttpTest`.
The HTTP suite uses the real generated client and adapter with a local Product
HTTP server, checking tenant paths, forwarded headers, isolation, response models
and failures.
`ProductServiceImplTest` also captures formatted lookup logs to verify that
identifiers stay on one line without changing the values sent to Product API.

### Coverage

Coverage combines ordinary JUnit tests with `@QuarkusTest`. The Maven JaCoCo agent
excludes `*QuarkusClassLoader` to avoid instrumenting Quarkus classes twice; both
collectors append to `target/jacoco.exec`. The Quarkus report includes all
application packages, including client headers, mappers, registries and utilities.
Keep the agent version aligned with the JaCoCo runtime supplied by Quarkus.
The collector settings live in test resources without a `%test` prefix so custom
profiles such as `integrationProfile` use the same data file and report scope.

Use a clean run to discard previous execution data and match the Sonar build:

```shell
mvn -B -ntp clean test --projects apps,apps/onboarding-ms --also-make \
  -Dquarkus.http.test-port=0 -Dquarkus.management.test-port=0
```

The module report is `apps/onboarding-ms/target/jacoco-report/jacoco.xml`.
The Sonar workflow imports module reports, not the separate `test-coverage`
aggregate. A selected run containing only ordinary JUnit tests does not start the
Quarkus report generator: include a `@QuarkusTest` or use the complete run above.

### Cucumber

The Cucumber suite also uses the real `ProductServiceImpl` and generated
`ProductApi`. Product MS responses are declared in
`src/test/resources/mock/product-api.json` and served by the existing MockServer,
alongside the User Registry and Party Registry mocks. There is no Product CDI
alternative, legacy catalog loader or duplicated Product business logic.

Add explicit expectations for the HTTP method, tenant path/header and required
query parameters. Responses use API-native JSON; the required-documents HEAD
response has no body and sets `X-Required-Documents-Enabled`. Every configured
response carries `X-Product-Mock: matched`. After each scenario, the suite checks
recorded Product responses for that marker, so an unconfigured request cannot
silently pass a negative test through MockServer's default 404. Only Product
request logs are cleared between scenarios; other services' expectations remain.

The Cucumber suite is selected explicitly in CI:

```shell
APP_SERVER_PORT=8082 mvn --projects :onboarding-ms --also-make test \
  -Dtest=it.pagopa.selfcare.onboarding.steps.OnboardingStep \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Check Cucumber's scenario report as well as Surefire's wrapper result. These tests
exercise onboarding's HTTP integration against explicit Product responses, not
Product MS's server-side business logic; that remains covered by `apps/product`.

## Cucumber integration tests in IntelliJ

The Cucumber suite starts its own Testcontainers Compose stack: MongoDB on port
`28017`, Azurite, MockServer and Document MS. Product API HTTP checks use a dedicated
local server instead of the old unused Product MS container. Docker must be running
and able to pull the required images (access to `ghcr.io/pagopa` may be required).

> **Test data only:** all environment variables, keys, tokens, connection strings, fixtures and databases used by the suite are fake/local test data. MongoDB and Azurite run in Docker and are not connected to Azure or to a real database.

### Fallback when Testcontainers fails

If Testcontainers cannot start the stack, temporarily comment the `ComposeContainer` creation, `start()` and shutdown-hook lines in [`OnboardingStep.setup()`](src/test/java/it/pagopa/selfcare/onboarding/steps/OnboardingStep.java). Do not commit that local change.

Then start the same stack manually from the repository root:

```shell
docker compose -f apps/onboarding-ms/src/test/resources/docker-compose.yml up
```

Wait for the `azure-cli` service to log `BLOBSTORAGE INITIALIZED`, then run the IntelliJ Cucumber configuration. Stop the manually managed stack at the end:

```shell
docker compose -f apps/onboarding-ms/src/test/resources/docker-compose.yml down
```

Create a **Cucumber Java** configuration with these values (the shared configuration is [Feature_ onboarding-ms.run.xml](../../.run/Feature_%20onboarding-ms.run.xml)):

| Field | Value |
| --- | --- |
| Feature file | `apps/onboarding-ms/src/test/resources/features/onboarding.feature` |
| Main class | `it.pagopa.selfcare.onboarding.steps.OnboardingStep` |
| Module | `onboarding-ms` |
| Working directory | module directory (`apps/onboarding-ms`) |
| Program arguments | `--plugin teamcity` (optional) |

The runner selects the `IntegrationProfile`, which uses test properties and local
HTTP expectations under `src/test/resources/mock`; ProductApi points to
`http://localhost:1080` through `src/test/resources/application.properties`.
The runner waits for the mock and checks
that Product expectations are loaded. The `product-catalog.feature` scenarios
exercise tenant-specific responses and cross-tenant not-found through the real
adapter. Azurite remains available for the other services in the stack. To run the readiness
scenarios, select `apps/onboarding-ms/src/test/resources/features/health.feature`;
the runner includes both `@Onboarding` and `@Health` tags. Readiness no longer
contains the removed `blob-storage-product` check.

No environment variables or Azure credentials are required for these Cucumber configurations. Keep environment-specific keys, connection strings and URLs out of shared IntelliJ configurations.

## Related Guides


### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### OpenAPI Generator

Rest client are generated using a quarkus' extension.

[Related guide section...](hhttps://github.com/quarkiverse/quarkus-openapi-generator)
