# Selfcare Onboarding BFF

Backend-for-frontend of the onboarding flow: it orchestrates the onboarding, institution, product, user and
token APIs of the Selfcare platform and exposes the `/v1` and `/v2` REST API used by the onboarding web application.

It is a Quarkus (Java 17) application living in the Selfcare monorepo (`apps/onboarding-bff`). It migrates the
Spring Boot `selfcare-onboarding-bff`, whose externally visible behavior (routes, parameters, status codes, error
bodies, headers, security, calls to the downstream services) must stay identical.

> **Migration status.** This document does not state that the migration is complete. The equivalence with the
> Spring BFF is established only by the gates described in
> [Verifying the equivalence with the Spring BFF](#verifying-the-equivalence-with-the-spring-bff); a red gate is a
> difference still to fix, never an expectation to relax. The matching OpenAPI document alone is not a proof.

Architectural realignment is tracked in the [migration plan](plan.md), with stories, implementation tasks,
dependencies and acceptance criteria. It is a backlog, not evidence that the refactoring or release is complete.

ST01/ST02/ST03 are complete. All product operations compose `Uni` from the REST client through service
and controller, including single-product lookup, required documents and their institution/token consumers.
Enabled and tax-code checks remain sequential even when enabled is true; USER uploads never acquire the
SYSTEM valid-product lookup. Only genuinely synchronous registry/party/document HTTP and file I/O use
explicit worker boundaries. The multipart attachment endpoint remains `@Blocking` while it reads the file;
moving that I/O out of the controller is still ST05, not completed by ST03.

The ST03 candidate passes the complete 550-scenario catalog on both untouched Spring main and Quarkus,
and all 63 Cucumber scenarios. The published OpenAPI, legacy alias and Spring golden are unchanged.
The bodyless verification GET explicitly declares `Void` in its response schema so `Uni<Response>` does
not introduce inferred JSON content.

ST04-T01/T03 are complete. All 27 InstitutionService methods and their controller consumers return
`Uni`, including company verification/onboarding, IPA queries, billing/geographies, active onboarding,
CSV verification, recipient code and document-gate triggering. Six native onboarding-ms calls compose
directly without await; the synchronous CSV upload and party/registry/manager lookups run on explicit
workers. Company ownership precedes onboarding, billing precedes institution/location lookup, and
only the original location 404 is recovered. Existing retries, tenant/header propagation, IAM empty
query omission and the user-registry PATCH rejection are preserved.

The ST04 institution candidate passes all 555 scenarios on both untouched Spring main and Quarkus,
and all 63 Cucumber scenarios. Canonical OpenAPI, alias and golden remain unchanged. The bodyless
users-PG POST also explicitly declares `Void`; multipart CSV verification remains `@Blocking`
until ST05 moves file reading out of controllers. ST04 remains open for users/IAM (T02) and the
story-wide final verification (T04); ST05-ST10 and release/review gates are not complete.

### Contract download authorization

`GET /v2/tokens/{onboardingId}/contract` requires authentication but does not query IAM or
onboarding-ms. It downloads the contract directly from document-ms, as required by the
onboarding link flow. It is not an anonymous endpoint.

`GET /v2/tokens/{onboardingId}/backstage/contract` retains the
`Selc:ViewAccountDocuments` authorization check. When IAM denies a view permission,
the onboarding requester identified by `userRequester.userRequestUid` or an onboarding
user can still view it. Management permissions never use that fallback.
Both routes preserve the binary body, filename and response headers.

## Build and run

Run the commands from the repository root. Maven needs the credentials of the `selfcare-platform` (Azure DevOps) and
`selfcare` (GitHub Packages) repositories in `~/.m2/settings.xml`.

| Goal | Command |
|------|---------|
| Build without tests | `mvn -f apps/onboarding-bff/pom.xml clean package -DskipTests` |
| Run the packaged application | `JWT_TOKEN_PUBLIC_KEY="$(cat public-key.pem)" java -jar apps/onboarding-bff/target/quarkus-app/quarkus-run.jar` |
| Dev mode with live reload | `JWT_TOKEN_PUBLIC_KEY=... mvn -f apps/onboarding-bff/pom.xml quarkus:dev` |
| Container image | `docker build -f apps/onboarding-bff/Dockerfile --build-arg REPO_SELFCARE=selfcare --build-arg REPO_USERNAME=<user> --build-arg REPO_PASSWORD=<token> .` |

The build regenerates the OpenAPI document under `apps/onboarding-bff/src/main/docs/` (`openapi.json`,
`openapi.yaml`): it is the canonical published specification and must be committed together with the code that
changes it, never edited by hand.

During `package`, Maven also copies `openapi.json` byte-for-byte to the legacy
`apps/onboarding-bff/app/src/main/resources/swagger/api-docs.json` path consumed by APIM and the frontend sync.
Commit both generated JSON files together; do not maintain the legacy alias manually.

The container image runs `java $JAVA_OPTIONS -jar /app/quarkus-run.jar` as user `1001` on port `8080`. The
Application Insights agent (3.7.8, the version of the other services) is in `/app/applicationinsights-agent.jar` and
is attached by the container app environment (`JAVA_TOOL_OPTIONS=-javaagent:applicationinsights-agent.jar`, relative to
the working directory `/app`), not by the image itself. The container app probes `actuator/health` on port `8080`
(liveness, readiness and startup).

## Configuration

Everything is configured through environment variables, read from `src/main/resources/application.properties`.

#### Server, logging, security

| **Environment variable** | **Default** | **Required** |
|--------------------------|-------------|:------------:|
|B4F_ONBOARDING_SERVER_PORT|8080| no |
|QUARKUS_LOG_LEVEL|INFO| no |
|B4F_ONBOARDING_LOG_LEVEL (level of `it.pagopa.selfcare`)|DEBUG| no |
|JWT_TOKEN_PUBLIC_KEY (legacy alias `JWT_PUBLIC_KEY`): public key verifying the signature of the bearer tokens|none, the application does not boot without it| yes |
|APPLICATIONINSIGHTS_CONNECTION_STRING|a zeroed instrumentation key| no |
|HTTP_IDLE_TIMEOUT, HTTP_READ_TIMEOUT|60s| no |
|HTTP_SO_REUSE_PORT, HTTP_TCP_CORK, TCP_QUICK_ACK, VERTX_PREFER_NATIVE_TRANSPORT|true| no |

`REST_CLIENT_LOGGER_LEVEL`, set by the infrastructure for the Spring BFF, is not read by the Quarkus application.

#### Downstream services

As in the Spring BFF, every URL has a default in every profile, `prod` included: a missing variable does not stop
the startup, the calls go to the default. The infrastructure sets the URLs in every environment (the PNPG environments
do not set `MS_DOCUMENT_URL`).

| **Environment variable** | **Downstream** | **Default** |
|--------------------------|----------------|-------------|
|MS_ONBOARDING_URL|onboarding-ms|http://localhost:8085|
|MS_USER_URL|user-ms|http://localhost:8080|
|MS_USER_INSTITUTION_URL|user-ms, institution API|http://localhost:8080|
|MS_PRODUCT_URL|product-ms|http://localhost:8080|
|MS_CORE_URL|institution-ms|http://10.1.1.250:80/ms-core/v1|
|MS_IAM_URL|iam|http://localhost:8080|
|MS_DOCUMENT_URL|document-ms|http://localhost:8080|
|USERVICE_PARTY_PROCESS_URL|party process|http://localhost:8080/pdnd-interop-uservice-party-process/0.0.1|
|USERVICE_PARTY_REGISTRY_PROXY_URL|party registry proxy|http://localhost:8080/external/ur/v1|
|USERVICE_USER_REGISTRY_URL|user registry|http://localhost:8080/pdnd-interop-uservice-user-registry/0.0.1|
|ONBOARDING_FUNCTIONS_URL|onboarding functions|https://localhost:8080|
|USERVICE_USER_REGISTRY_API_KEY (alias `USER-REGISTRY-API-KEY`)|user registry key|api-key|
|ONBOARDING-FUNCTIONS-API-KEY|onboarding functions key|example-api-key|

#### REST client timeouts (milliseconds)

Every REST client uses **10000 ms connect / 60000 ms read**, including aggregates and institution calls.
These values preserve the measured behavior of the Spring Feign clients: Spring's legacy
`feign.client.config.*` settings were not bound by OpenFeign 4.

The legacy `REST_CLIENT_*`, `USERVICE_*_REST_CLIENT_*`, `IAM_REST_CLIENT_*` and `AGGREGATES_REST_CLIENT_*`
timeout variables do not override these settings. For an intentional override, use the corresponding Quarkus
property, for example `-Dquarkus.rest-client.iam_json.read-timeout=1500`. The client configuration keys and
fully qualified API-specific overrides are listed in `src/main/resources/application.properties`.

## Tests

All the commands run from the repository root. The unit tests, the parity gates and the OpenAPI gates are plain
Surefire tests, so a standard `mvn verify` runs all of them; only the Cucumber suite needs Docker and is a separate
Failsafe run.

| Goal | Command |
|------|---------|
| Unit tests and gates | `mvn -f apps/onboarding-bff/pom.xml test` |
| One class or method | `mvn -f apps/onboarding-bff/pom.xml test -Dtest=OpenApiInventoryTest` |
| Cucumber integration suite (Docker required) | `mvn -f apps/onboarding-bff/pom.xml verify -Pintegration-tests` |
| Coverage report | `mvn --projects :test-coverage --also-make verify -Ponboarding-bff,report` |

When testing this module together with other Quarkus applications in the same Maven reactor, use `-T 1`.
The mixed Quarkus plugin versions currently fail BFF test-class discovery in parallel reactor builds.
The monorepo Sonar workflow selects serial execution whenever its scope includes onboarding-bff; all tests
and coverage collection remain enabled.

The suite is `CucumberSuiteTest`; without `-Pintegration-tests` Failsafe is skipped (`skipITs=true`). The scenarios
start the compose stack in `src/test/resources/docker-compose.yml` (MongoDB, Azurite, mock server and the images of the
downstream services) and call the BFF with signed tokens.
The pinned downstream images use tenant-aware MongoDB configuration for the existing fixture databases.
Both tenants share the SDK's legacy test signing key; the registry deliberately leaves `jwt` unset so the
downstream verifier uses that single key, rather than synthetic tenant key IDs incompatible with `jwt_test_kid`.
Signature verification and tenant validation remain enabled.
Mongo fixtures carry the SDK users' `AR` tenant. Product-ms is seeded from the same legacy catalog mounted
for blob initialization, mapped to its current schema: 14 ACTIVE products, including disabled products,
and 11 ACTIVE roots, of which 5 have the default user contract required by the admin endpoint.
The Cucumber assertions cover the exact public/admin counts.
IAM fixtures require the decoded permission path, the `AR` tenant and the seeded product IDs.
They also require `institutionId` to be absent: Spring's product-scoped permission lookup passes an
empty value internally, which Feign omits on the wire. The Quarkus client preserves that omission;
the fixtures do not grant access for arbitrary institution or product contexts.

## Verifying the equivalence with the Spring BFF

The reference is the Spring BFF, which stays untouched in its own checkout. The same scenario catalog
(`src/test/java/it/pagopa/selfcare/onboarding/parity`) is run, over real HTTP with really signed JWTs and the downstream
services replaced by a controlled in-JVM stub, against both applications:

1. **Spring oracle.** Every expectation is first validated on the unchanged Spring executable jar, so that an expected
   value is observed, never assumed. Build the baseline from the Spring checkout
   (`mvn -f apps/onboarding-bff/pom.xml -pl app -am clean package -DskipTests`): the executable jar is
   `apps/onboarding-bff/target/onboarding-bff-<version>-FATJAR.jar` (the plain `app/target/*.jar` has no main
   manifest and cannot be used). Then, from this repository:

   ```shell
   mvn -f apps/onboarding-bff/pom.xml test -Dtest=SpringReferenceParityTest \
     -Dparity.spring.jar=/path/to/selfcare-bff-spring-baseline/apps/onboarding-bff/target/onboarding-bff-<version>-FATJAR.jar
   ```

   After merging upstream behavior changes, build a new untouched Spring oracle from the
   merged `main` revision and record its SHA-256 in the migration plan. Keep previous
   oracle jars unchanged; never certify new upstream routes against an older executable.

   Without `-Dparity.spring.jar` the class is reported as skipped with an explicit reason, and `ParityCatalogTest`
   keeps failing the build if the catalog shrinks below its validated size.
2. **Quarkus.** `QuarkusParityTest` runs the same catalog against this application: status, error body and
   content type, `401`/`403`, tenant propagation, downstream calls and their number, multipart parts, downloads,
   timeouts. It runs with the other unit tests, with no extra option. A failure is a difference of the Quarkus BFF.
   Use `-Dparity.only='<regex on the scenario name>'` (for example `-Dparity.only='^security ::'`) to iterate on a group.
3. **Contract documents.** `OpenApiInventoryTest` tests the comparator itself; `QuarkusOpenApiInventoryTest` and
   `PublishedOpenApiGateTest` compare the specification served at `/v3/api-docs` and the one committed in
   `src/main/docs` with the Spring one (`src/test/resources/parity/spring-api-docs.json`, taken from the Spring
   checkout): routes, methods, parameters and their style, required flags, schemas, response headers, security,
   operation ids. These gates are necessary but not sufficient: the behavior is proven by the scenarios.
   `OpenApiIdenticalDocumentGateTest` additionally requires exact structural equality with the Spring document;
   `LegacyOpenApiAliasGateTest` checks the legacy alias against the generated and served document, including its
   suitability for Terraform `templatefile`.

Each run prints a line `PARITY <target> scenarios=<n> attempted=<n> passed=<n>`; a run that executes no scenario fails.

Compatibility includes the Spring user-registry client's HTTP method restriction: its `HttpURLConnection`
transport rejects PATCH before sending a request. The Quarkus client preserves that error, including the
onboarding response and absence of downstream writes. Enabling user creation/update via PATCH is a separate
functional change, not an implicit fix bundled into this migration.

`HttpsParityTest` additionally opens real TLS listeners for both runtimes using a short-lived local
certificate trusted only by its test client. It verifies HSTS on protected success/error responses and its
absence on public health. HTTP scenarios verify that untrusted forwarded headers do not enable HSTS.
These are application-boundary checks: APIM TLS termination, trusted proxy configuration and deployed
forwarding policies still require the authorized environment smoke checks.

Coverage combines plain JUnit and `@QuarkusTest` executions in `target/jacoco.exec`: the Maven JaCoCo agent
instruments ordinary classloaders, while `quarkus-jacoco` instruments the Quarkus classloader. Both cover the
whole BFF package and append to the shared data file. Use `clean` for a fresh measurement; the existing
`test-coverage` aggregate consumes that file without changing the Sonar quality gate or report exclusions.

## Core Configurations (legacy Spring BFF)

The Spring BFF documented here its feign clients with `feign.client.config.*` properties; they do not exist in the
Quarkus application, whose equivalents are the environment variables above.
|rest-client.party-process.base-url|USERVICE_PARTY_PROCESS_URL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/party-process-rest-client.properties)| yes |
|feign.client.config.party-process.connectTimeout|USERVICE_PARTY_PROCESS_REST_CLIENT_CONNECT_TIMEOUT<br>REST_CLIENT_CONNECT_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/party-process-rest-client.properties)| yes |
|feign.client.config.party-process.readTimeout|USERVICE_PARTY_PROCESS_REST_CLIENT_READ_TIMEOUT<br>REST_CLIENT_READ_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/party-process-rest-client.properties)| yes |
|feign.client.config.party-process.loggerLevel|USERVICE_PARTY_PROCESS_REST_CLIENT_LOGGER_LEVEL<br>REST_CLIENT_LOGGER_LEVEL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/party-process-rest-client.properties)| yes |
|rest-client.products.base-url|MS_PRODUCT_URL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/products-rest-client.properties)| yes |
|feign.client.config.products.connectTimeout|MS_PRODUCT_REST_CLIENT_CONNECT_TIMEOUT<br>REST_CLIENT_CONNECT_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/products-rest-client.properties)| yes |
|feign.client.config.products.readTimeout|MS_PRODUCT_REST_CLIENT_READ_TIMEOUT<br>REST_CLIENT_READ_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/products-rest-client.properties)| yes |
|feign.client.config.products.loggerLevel|MS_PRODUCT_REST_CLIENT_LOGGER_LEVEL<br>REST_CLIENT_LOGGER_LEVEL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/products-rest-client.properties)| yes |
|rest-client.user-registry.base-url|USERVICE_USER_REGISTRY_URL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |
|feign.client.config.user-registry.defaultRequestHeaders.x-api-key|USERVICE_USERVICE_USER_REGISTRY_API_KEY|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |
|feign.client.config.user-registry.connectTimeout|USERVICE_USER_REGISTRY_REST_CLIENT_CONNECT_TIMEOUT<br>REST_CLIENT_CONNECT_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |
|feign.client.config.user-registry.readTimeout|USERVICE_USER_REGISTRY_REST_CLIENT_READ_TIMEOUT<br>REST_CLIENT_READ_TIMEOUT|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |
|feign.client.config.user-registry.loggerLevel|USERVICE_USER_REGISTRY_REST_CLIENT_LOGGER_LEVEL<br>REST_CLIENT_LOGGER_LEVEL|<a name= "default property"></a>[default_property](https://github.com/pagopa/selfcare-onboarding-bff/blob/main/connector/rest/src/main/resources/config/user-registry-rest-client.properties)| yes |


#### Core Configurations

| **Property** | **Enviroment Variable** | **Default** | **Required** |
|--------------|-------------------------|-------------|:------------:|
