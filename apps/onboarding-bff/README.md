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

In the `prod` profile the URLs set by the infrastructure in every environment are mandatory: the application fails to
start listing the missing ones, instead of routing the calls to its own `localhost` default.

| **Environment variable** | **Downstream** | **Default (not `prod`)** | **Required in `prod`** |
|--------------------------|----------------|--------------------------|:----------------------:|
|MS_ONBOARDING_URL|onboarding-ms|http://localhost:8085| yes |
|MS_USER_URL|user-ms|http://localhost:8080| yes |
|MS_USER_INSTITUTION_URL|user-ms, institution API|http://localhost:8080| yes |
|MS_PRODUCT_URL|product-ms|http://localhost:8080| yes |
|MS_CORE_URL|institution-ms|http://10.1.1.250:80/ms-core/v1| yes |
|MS_IAM_URL|iam|http://localhost:8080| yes |
|MS_DOCUMENT_URL|document-ms|http://localhost:8080| no (the PNPG environments do not set it) |
|USERVICE_PARTY_PROCESS_URL|party process|http://localhost:8080/pdnd-interop-uservice-party-process/0.0.1| yes |
|USERVICE_PARTY_REGISTRY_PROXY_URL|party registry proxy|http://localhost:8080/external/ur/v1| yes |
|USERVICE_USER_REGISTRY_URL|user registry|http://localhost:8080/pdnd-interop-uservice-user-registry/0.0.1| yes |
|ONBOARDING_FUNCTIONS_URL|onboarding functions|https://localhost:8080| yes |
|USERVICE_USER_REGISTRY_API_KEY (alias `USER-REGISTRY-API-KEY`)|user registry key|api-key| no |
|ONBOARDING-FUNCTIONS-API-KEY|onboarding functions key|example-api-key| no |

#### REST client timeouts (milliseconds)

`REST_CLIENT_CONNECT_TIMEOUT` (default 10000) and `REST_CLIENT_READ_TIMEOUT` (default 60000) apply to every client;
a client-specific variable takes precedence.

| **Client** | **Connect timeout variable** | **Read timeout variable** |
|------------|------------------------------|---------------------------|
|party process, onboarding, user, party registry proxy|USERVICE_PARTY_PROCESS_REST_CLIENT_CONNECT_TIMEOUT|USERVICE_PARTY_PROCESS_REST_CLIENT_READ_TIMEOUT|
|institution (core)|USERVICE_MS_CORE_REST_CLIENT_CONNECT_TIMEOUT|USERVICE_MS_CORE_REST_CLIENT_READ_TIMEOUT|
|iam|IAM_REST_CLIENT_CONNECT_TIMEOUT|IAM_REST_CLIENT_READ_TIMEOUT|
|user registry|USERVICE_USER_REGISTRY_REST_CLIENT_CONNECT_TIMEOUT|USERVICE_USER_REGISTRY_REST_CLIENT_READ_TIMEOUT|
|aggregates and institution calls of onboarding-ms (default 90000)|AGGREGATES_REST_CLIENT_CONNECT_TIMEOUT|AGGREGATES_REST_CLIENT_READ_TIMEOUT|

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

The suite is `CucumberSuiteTest`; without `-Pintegration-tests` Failsafe is skipped (`skipITs=true`). The scenarios
start the compose stack in `src/test/resources/docker-compose.yml` (MongoDB, Azurite, mock server and the images of the
downstream services) and call the BFF with signed tokens.

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

Each run prints a line `PARITY <target> scenarios=<n> attempted=<n> passed=<n>`; a run that executes no scenario fails.

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
