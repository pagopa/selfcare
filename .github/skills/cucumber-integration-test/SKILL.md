---
name: cucumber-integration-test
description: "Add or modify Cucumber BDD integration tests in Selfcare Quarkus microservices. USE WHEN the user asks to write a new .feature file, add a Cucumber scenario, wire up a new microservice for integration tests, set up Testcontainers/docker-compose for tests, generate a JWT for a Cucumber step, or debug a failing `mvn verify` on an app under `apps/`. TRIGGER WORDS: cucumber, feature file, BDD, integration test, CucumberSuiteTest, IntegrationProfile, Testcontainers, mvn verify, RestAssured, selfcare-cucumber-sdk."
---

> **Skill-usage marker (diagnostic):** when you apply this skill in a reply, begin the reply with the exact literal marker `[SKILL:cucumber-integration-test]` on its own line. This lets the user verify the skill was actually loaded.

# Selfcare Cucumber Integration Tests

Selfcare microservices (Quarkus 3.31, Java 17) use **Cucumber BDD** for end-to-end integration tests, orchestrated via **Quarkus + Testcontainers + RestAssured** and glued together by the shared library `libs/selfcare-cucumber-sdk` (Maven coordinates `it.pagopa.selfcare:selfcare-cucumber-sdk`, currently `0.1.3`).

This skill teaches you how to (a) add a new `.feature` file to an existing microservice, (b) add app-specific step definitions, and (c) bootstrap Cucumber tests from scratch in a microservice that doesn't have them yet.

## 1. Standard layout in an app

Each microservice that uses Cucumber follows the same layout:

```
apps/<app>/
├── pom.xml                              # failsafe + quarkus-cucumber + selfcare-cucumber-sdk (test scope)
└── src/test/
    ├── java/it/pagopa/selfcare/<app>/integrationTest/   # (some apps use .../cucumber/)
    │   ├── CucumberSuiteTest.java       # JUnit entry point (extends CucumberQuarkusTest)
    │   ├── IntegrationProfile.java      # QuarkusTestProfile — injects JWT public key etc.
    │   └── <Domain>Steps.java           # OPTIONAL app-specific step definitions
    └── resources/
        ├── application.properties       # test-time Quarkus config
        ├── cucumber.properties          # e.g. `cucumber.publish.enabled=true`
        ├── docker-compose.yml           # services started by Testcontainers ComposeContainer
        ├── Dockerfile-azure-cli         # optional, used by azure-cli seed container
        ├── certs/pk-key.pub             # JWT public key (paired with SDK private key)
        ├── db/                          # optional MongoDB init scripts (mounted into /docker-entrypoint-initdb.d)
        ├── blobStorage/                 # optional Azurite seed data
        └── features/
            ├── <feature-1>.feature
            └── <feature-2>.feature
```

Reference implementation: `apps/product/src/test/**`.

## 2. What the shared SDK gives you for free

`it.pagopa.selfcare.cucumber.utils` (in `libs/selfcare-cucumber-sdk`) provides three CDI beans that are auto-discovered when you add the SDK as a `test` dependency AND include the SDK's package in the `glue` list of `@CucumberOptions`:

| Class | Purpose |
|---|---|
| `CommonSteps` | Ready-to-use Given/When/Then steps for login, request building (path/query/form/body/multipart), HTTP verbs (GET/POST/PUT/PATCH/HEAD/DELETE), and response assertions (status, headers, body, JSON path, list comparison). |
| `SharedStepData` | `@ApplicationScoped` bean that carries the current JWT token, request payload, path/query params, file upload descriptor and last response between steps in the same scenario. Cleared automatically before each scenario via `@Before`. |
| `TestJwtGenerator` | Signs test JWTs with the SDK-bundled RSA key (`key/private-key.pem` on the SDK classpath). Reads user profiles from `testData.json` (also on the SDK classpath). |

**You almost never need to duplicate HTTP or auth steps** — the SDK covers the common CRUD flow. Write app-specific step classes only when the domain needs it (seeding Mongo, custom assertions, external mocks, etc.).

### Built-in step vocabulary (from `CommonSteps`)

Use these verbatim in `.feature` files:

```gherkin
Given User login with username "j.doe" and password "test"
Given A bad jwt token
And   The following request body:
      """
      { ... json ... }
      """
And   The following path params:
      | key | value |
And   The following query params:
      | key | value |
And   The following form data:
      | key | value |
Given The following form params:
      | key | value |
And   Upload the file at path "path/on/classpath" with form key "file" and content type "application/pdf"
And   A mock-file of type "application/pdf" with key "file" and document path "documents/foo.pdf" used to perform request

When  I send a GET    request to "/product/{id}"
When  I send a POST   request to "/product"
When  I send a POST   request to "/upload" with form data and multi-part file
When  I send a POST   request to "/upload" with form data only
When  I send a POST   request to "/upload" with multi-part file
When  I send a PUT    request to "/product/{id}"
When  I send a PATCH  request to "/product/{id}" with content type "application/json"
When  I send a HEAD   request to "/product/{id}/available"
When  I send a DELETE request to "/product/{id}"

Then  The status code is 200
And   The response header contains:
      | X-Some-Header | value |
And   The response body contains:
      | jsonPath | expectedValue |
And   The response body does not contain:
      | jsonPath | unexpectedValue |
And   The response body contains the string "..."
And   The response body contains string:
      """ literal body """
And   The response body contains field "jsonPath"
And   The response body doesn't contain field "jsonPath"
And   The response body contains the list "items" of size 3
And   The response body contains at path "items" the following list of values in any order:
      | v1 |
      | v2 |
And   The response body contains at path "items" the following list of objects in any order:
      | id  | name |
      | 1   | foo  |
```

## 3. Add a new `.feature` file to an existing app

1. Drop the file in `apps/<app>/src/test/resources/features/<name>.feature`.
2. Reuse the built-in vocabulary above. Prefer plain Gherkin — DO NOT write new step methods unless a step really doesn't exist.
3. Path/query/body values that must survive across scenarios go through `SharedStepData` (already handled by `CommonSteps`).
4. Test data files referenced by `Upload the file at path ...` must live under `src/test/resources/` (loaded via classpath).
5. Run only your feature quickly:
   ```bash
   mvn -f apps/<app>/pom.xml verify -DskipITs=false -Dcucumber.filter.tags="@my-tag"
   ```
   (Tag your scenarios with `@my-tag` first.) To run the entire suite: `mvn -f apps/<app>/pom.xml verify -DskipITs=false`.

## 4. Add app-specific step definitions

Only when a step is not provided by `CommonSteps`. Create a class next to `CucumberSuiteTest.java`:

```java
package it.pagopa.selfcare.<app>.integrationTest;

import io.cucumber.java.en.Given;
import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductSeedSteps {

  private final SharedStepData shared;

  public ProductSeedSteps(SharedStepData shared) {
    this.shared = shared;
  }

  @Given("A product named {string} already exists")
  public void seedProduct(String name) {
    // e.g. call the Mongo test container, seed a document, or hit an admin endpoint
  }
}
```

The class is picked up because its package is listed in `@CucumberOptions(glue = { "it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.<app>.integrationTest" })` on `CucumberSuiteTest`. If you put the step class in a different package, ADD that package to the `glue` array.

Reuse `SharedStepData` via constructor injection to read/write the current token, request body, or last response.

## 5. Bootstrap Cucumber in a microservice that doesn't have it yet

### 5.1 `pom.xml` — add these entries

Under `<properties>`:
```xml
<skipITs>true</skipITs>
<surefire-plugin.version>3.5.4</surefire-plugin.version>
<quarkus-cucumber-version>1.3.0</quarkus-cucumber-version>
<selfcare-cucumber-sdk.version>0.1.3</selfcare-cucumber-sdk.version>
```

Under `<dependencies>` (all `test` scope):
```xml
<dependency>
  <groupId>it.pagopa.selfcare</groupId>
  <artifactId>selfcare-cucumber-sdk</artifactId>
  <version>${selfcare-cucumber-sdk.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.quarkiverse.cucumber</groupId>
  <artifactId>quarkus-cucumber</artifactId>
  <version>${quarkus-cucumber-version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.rest-assured</groupId>
  <artifactId>rest-assured</artifactId>
  <scope>test</scope>
</dependency>
```

Under `<build><plugins>` — Failsafe (runs `verify`, honors `skipITs`) AND Surefire exclusion so unit tests don't try to boot the Cucumber suite:

```xml
<plugin>
  <artifactId>maven-failsafe-plugin</artifactId>
  <version>${surefire-plugin.version}</version>
  <executions>
    <execution>
      <goals><goal>integration-test</goal><goal>verify</goal></goals>
      <configuration>
        <systemPropertyVariables>
          <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
          <maven.home>${maven.home}</maven.home>
        </systemPropertyVariables>
      </configuration>
    </execution>
  </executions>
  <configuration>
    <skip>${skipITs}</skip>
    <systemPropertyVariables>
      <user.language>en</user.language>
      <user.country>US</user.country>
      <user.timezone>GMT</user.timezone>
    </systemPropertyVariables>
  </configuration>
</plugin>
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>${surefire-plugin.version}</version>
  <configuration>
    <excludes>
      <exclude>**/CucumberSuiteTest.java</exclude>
    </excludes>
  </configuration>
</plugin>
```

### 5.2 `CucumberSuiteTest.java` — copy this template verbatim, change only package + glue

```java
package it.pagopa.selfcare.<app>.integrationTest;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import java.io.File;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@Slf4j
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.<app>.integrationTest"},
    plugin = {
      "html:target/cucumber-report/cucumber.html",
      "json:target/cucumber-report/cucumber.json"
    })
@TestProfile(IntegrationProfile.class)
public class CucumberSuiteTest extends CucumberQuarkusTest {

  public static void main(String[] args) { runMain(CucumberSuiteTest.class, args); }

  @BeforeAll
  static void setup() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8081;                                    // Quarkus test port
    var compose = new ComposeContainer(new File("./src/test/resources/docker-compose.yml"))
        .withPull(true)
        .waitingFor("mongodb", Wait.forListeningPort())          // add one waitingFor per service
        .withStartupTimeout(Duration.ofMinutes(5));
    compose.start();
    Runtime.getRuntime().addShutdownHook(new Thread(compose::stop));
  }

  @AfterAll static void tearDown() { log.info("Cucumber tests are finished."); }
}
```

### 5.3 `IntegrationProfile.java` — injects the JWT public key that pairs with the SDK's private key

```java
package it.pagopa.selfcare.<app>.integrationTest;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class IntegrationProfile implements QuarkusTestProfile {
  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of("mp.jwt.verify.publickey", getPublicKey());
  }

  private String getPublicKey() {
    try {
      return new String(Files.readAllBytes(new File("src/test/resources/certs/pk-key.pub").toPath()));
    } catch (Exception e) {
      log.error("Cannot read test JWT public key", e);
      return StringUtils.EMPTY;
    }
  }
}
```

Add other `.put(...)` overrides here for anything the app reads from config (Mongo URI, blob endpoint, feature flags) so it points at Testcontainers.

### 5.4 `src/test/resources/certs/pk-key.pub`

Must be the RSA public key that pairs with `libs/selfcare-cucumber-sdk/src/main/resources/key/private-key.pem` (the private key used by `TestJwtGenerator`). **Copy it from any existing app that already has integration tests** (e.g. `apps/product/src/test/resources/certs/pk-key.pub`) — do not regenerate.

### 5.5 `src/test/resources/cucumber.properties`

```properties
cucumber.publish.enabled=true
```

### 5.6 `src/test/resources/docker-compose.yml`

Minimal example (MongoDB only). Add services as the app needs — Azurite, azure-cli seeder, Redis, Kafka, WireMock, etc. Reference: `apps/product/src/test/resources/docker-compose.yml`.

```yaml
version: '3.8'
services:
  mongodb:
    image: mongo:latest
    ports:
      - '27017:27017'
    volumes:
      - "./db:/docker-entrypoint-initdb.d"
    networks:
      - develop-network
networks:
  develop-network:
    name: selfcare-develop-network
```

**Pin images by digest** in real setups (see how `apps/product` uses `mongo:latest@sha256:...`) to guarantee reproducibility.

Every service you list here that must be ready before Quarkus starts requires a matching `.waitingFor(...)` in `CucumberSuiteTest.setup()`.

### 5.7 First feature file

`src/test/resources/features/smoke.feature`:

```gherkin
Feature: Smoke check

  Scenario: Health endpoint is up
    Given User login with username "j.doe" and password "test"
    When I send a GET request to "/q/health"
    Then The status code is 200
```

## 6. Run

```bash
# Full IT suite for an app (Docker required)
mvn -f apps/<app>/pom.xml verify -DskipITs=false

# Whole repo IT
mvn verify -DskipITs=false

# Skip IT (default)
mvn -f apps/<app>/pom.xml verify
```

Cucumber HTML + JSON reports are written to `apps/<app>/target/cucumber-report/`.

## 7. Common pitfalls

- **`skipITs=true` is the default in every app pom** — you MUST pass `-DskipITs=false` to actually run integration tests.
- **Docker daemon must be running** locally; the `ComposeContainer` uses Docker Compose v2.
- **Port 8081** is the Quarkus test HTTP port — do not hardcode 8080 in feature URLs (use relative paths like `/product/...`).
- **Adding a new step package?** You must add it to the `glue` array in `@CucumberOptions`, otherwise Cucumber will report `undefined step`.
- **New service in docker-compose?** Add a `.waitingFor(...)` in `setup()` or the Quarkus app may start before dependencies are ready and fail on first request.
- **Do not commit the SDK private key** into an app — the private key lives ONLY in `libs/selfcare-cucumber-sdk/src/main/resources/key/private-key.pem`; each app only ships the matching public key at `src/test/resources/certs/pk-key.pub`.
- **Sonar** on the SDK is intentionally disabled (`<sonar.skip>true</sonar.skip>`) — do not enable it.
- If you bump `selfcare-cucumber-sdk.version` in one app, align every other app to the same version to avoid classpath conflicts.

## 8. Reference file paths

- Shared SDK: `libs/selfcare-cucumber-sdk/`
- Full working example: `apps/product/` (feature + steps + compose + certs)
- Other reference apps with Cucumber IT: `apps/webhook/`, `apps/iam/`, `apps/document-ms/`, `apps/onboarding-ms/`

