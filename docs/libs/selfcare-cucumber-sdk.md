# selfcare-cucumber-sdk

> Shared Cucumber step definitions and test utilities (RestAssured HTTP steps, test JWT generation, test users) for the BDD integration test suites of the SelfCare apps.

| | |
|---|---|
| Path | `libs/selfcare-cucumber-sdk` |
| Artifact | `it.pagopa.selfcare:selfcare-cucumber-sdk:0.1.3` (no parent; `libs/pom.xml` lists it as a module) |
| Stack | Java 17, Cucumber 7.20.1 (`cucumber-java`, `cucumber-jakarta-cdi`), RestAssured, Testcontainers 2.0.2 BOM, `java-jwt`, Lombok; beans for both CDI (Quarkus) and Spring |
| Used by (test scope) | `auth`, `dashboard-bff`, `document-ms`, `iam`, `institution-ms/app`, `onboarding-bff/app`, `product`, `product-cdc`, `user-group-ms`, `user-ms`, `webhook` |

Consumers pin different versions (e.g. `0.1.2` in `dashboard-bff`, `iam`, `product-cdc`; `0.1.3` in `auth`, `document-ms`,
`institution-ms/app`). `onboarding-functions` declares the `selfcare-cucumber-sdk.version` property but no dependency.

## What it provides

Package `it.pagopa.selfcare.cucumber.utils`:

| Class | Purpose |
|---|---|
| `CommonSteps` | Generic HTTP step definitions (see below). Clears shared state `@Before` each scenario. |
| `SharedStepData` | Per-run state: token, tenant id, request body, path/query params, form data, uploads, last response. |
| `TestDataProvider` | Loads `testData.json` from the classpath (`jwtData`: list of test users with JWT header/payload). |
| `TestJwtGenerator` | Signs RS256 tokens with the bundled **test** key `key/private-key.pem` (public key `key/public-key.pub`); `generateToken(JwtData)` expires after 1 hour, `generateToken(JwtData, Instant)` for custom expiry. |
| `model.JwtData`, `model.TestData`, `model.FileDescriptor` | Test data model. |

Bundled `testData.json` contains three users (`j.doe`, `r.balboa`, `user-002`), all with `tenant_id` `AR`. The key pair is
for tests only: the apps under test must trust the matching public key in their test configuration.

Main steps (`CommonSteps`):

- Auth: `User login with username {string} and password {string}` (generates a JWT for the matching test user and
  remembers its `tenant_id` claim), `A bad jwt token`.
- Request setup: `The following request body:`, `The following path params:`, `The following query params:`,
  `The following form data:`, `The following form params:`, `Upload the file at path {string} with form key {string} and content type {string}`,
  `A mock-file of type {string} with key {string} and document path {string} used to perform request`.
- Calls: `I send a GET|POST|PUT|HEAD|DELETE request to {string}`, POST variants `with form data and multi-part file`,
  `with form data only`, `with multi-part file`, `I send a PATCH request to {string} with content type {string}`.
  Requests carry the bearer token and, if known, the `X-Tenant-Id` header.
- Assertions: `The status code is {int}`, `The response header contains:`, `The response body contains:` / `does not contain:`,
  `contains the string {string}`, `contains field {string}` / `doesn't contain field {string}`,
  `contains the list {string} of size {int}`, `contains at path {string} the following list of values|objects in any order:`.

## Usage

```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-cucumber-sdk</artifactId>
  <version>0.1.3</version>
  <scope>test</scope>
</dependency>
```

Add the SDK package to the Cucumber glue of the app runner, e.g. `apps/auth/src/test/java/it/pagopa/selfcare/auth/integration_test/CucumberSuite.java`:

```java
glue = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.auth.integration_test"}
```

Spring apps also component-scan the package (see `apps/dashboard-bff/src/test/java/it/pagopa/selfcare/dashboard/integration_test/CucumberSuite.java`).
Quarkus apps discover the beans through `META-INF/beans.xml` (`bean-discovery-mode="annotated"`).

## Build and release

```bash
mvn --projects :selfcare-cucumber-sdk --also-make clean package -DskipTests
mvn --projects :selfcare-cucumber-sdk --also-make-dependents clean package -DskipTests   # lib + dependents
```

Release: `.github/workflows/release_selfcare_cucumber_sdk.yml` (push on `main` with path filter on the lib, or
`workflow_dispatch`) runs `mvn -B deploy -f libs/selfcare-cucumber-sdk/pom.xml` to `https://maven.pkg.github.com/pagopa/selfcare`
(the POM declares its own `distributionManagement` and `maven-deploy-plugin`). `sonar.skip=true`; coverage profile
`selfcare-cucumber-sdk` exists in `test-coverage/pom.xml`.

## References

- [Testing](../testing.md)
- No README in the lib folder.
