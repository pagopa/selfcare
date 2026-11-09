# Testing

The repository has four layers of tests: unit tests per module, Cucumber integration suites against
a local Docker stack, a Bruno contract collection run against deployed environments, and coverage
reported to SonarCloud. Build prerequisites (Java 17, Maven 3.9.x, `~/.m2/settings.xml` credentials)
are described in [ci-cd.md](./ci-cd.md) and `AGENTS.md`.

## Unit tests

- JUnit 5 + Mockito everywhere. Run one module with `mvn -f apps/<app>/pom.xml test`, or a single
  test with `-Dtest=MyServiceTest#myMethod`.
- **Quarkus apps** use `@QuarkusTest` with `@InjectMock` for CDI beans (never `@Mock` on beans).
  Reactive code is asserted with `UniAssertSubscriber` (auth, document-ms, iam, onboarding-ms,
  product, user-ms, webhook, the CDC services, ...) or `.await().indefinitely()`.
- **Spring apps** (dashboard-bff, external-api, institution-ms, onboarding-bff, registry-proxy,
  user-group-ms) use `@MockBean` for Spring beans and `@ExtendWith(MockitoExtension.class)`
  for plain unit tests.
- When touching a library in `libs/`, also build its dependents:
  `mvn --projects :<lib> --also-make-dependents clean package`.

## Cucumber integration suites

Feature files live in `src/test/resources/features/`. Each suite starts its dependencies (Mongo,
Azurite, mock servers) with Testcontainers and calls the real app. Test JWTs come from
[`libs/selfcare-cucumber-sdk`](./libs/selfcare-cucumber-sdk.md) (`TestJwtGenerator`, `CommonSteps`,
`SharedStepData`, `TestDataProvider`), signed with a test key pair, never with real credentials.

Many POMs exclude the runner from Surefire (auth, document-ms, iam, product, webhook), so a plain
`mvn test` does not run it. Select it explicitly:

```bash
mvn --projects :<app> --also-make test -Dtest=<Runner> -Dsurefire.failIfNoSpecifiedTests=false
```

`failIfNoSpecifiedTests=false` silently runs nothing if the class name is wrong: check that Surefire
reports `Tests run: N` with N > 0. The report is written to `target/cucumber-report/cucumber.html`.

| App | Runner (`-Dtest=`) | Docker stack | CI command (`.github/workflows/`) |
|-----|--------------------|--------------|------------------------------------|
| [auth](./apps/auth.md) | `it.pagopa.selfcare.auth.integration_test.CucumberSuite` | `src/test/resources/docker-compose.yml` (via `AuthIntegrationTestResource`) | `integration_tests_auth_ms.yml`: `mvn --projects :auth --also-make test -Dtest=CucumberSuite` |
| [iam](./apps/iam.md) | `it.pagopa.selfcare.iam.cucumber.CucumberSuiteTest` | `apps/iam/docker-compose.yml` | `integration_tests_iam_ms.yml`: `--projects :iam -Dtest=CucumberSuite` (differs from the runner class name) |
| [product](./apps/product.md) | `it.pagopa.selfcare.product.integrationTest.CucumberSuiteTest` | `src/test/resources/docker-compose.yml` | `integration_tests_product_ms.yml` |
| [document-ms](./apps/document-ms.md) | `it.pagopa.selfcare.document.integrationTest.CucumberSuiteTest` | `src/test/resources/docker-compose.yml` | `integration_tests_document_ms.yml` |
| [user-ms](./apps/user-ms.md) | `it.pagopa.selfcare.user.integration_test.CucumberSuite` | `apps/user-ms/docker-compose.yml` | `integration_tests_user_ms.yml` |
| [user-group-ms](./apps/user-group-ms.md) | `it.pagopa.selfcare.user_group.integration_test.CucumberSuite` | Mongo `GenericContainer` | `integration_tests_user_group_ms.yml` |
| [onboarding-ms](./apps/onboarding-ms.md) | `it.pagopa.selfcare.onboarding.steps.OnboardingStep` | `src/test/resources/docker-compose.yml` | `integration_tests_onboarding_ms.yml` |
| [onboarding-functions](./apps/onboarding-functions.md) | `it.pagopa.selfcare.onboarding.steps.OnboardingFunctionStep` | `src/test/resources/docker-compose.yml`, started by the workflow | `integration_tests_onboarding_functions.yml` (from `apps/onboarding-functions`) |
| [dashboard-bff](./apps/dashboard-bff.md) | `it.pagopa.selfcare.dashboard.integration_test.CucumberSuite` | `apps/dashboard-bff/docker-compose.yml` | `integration_tests_dashboard_bff.yml` (from repo root) |
| [onboarding-bff](./apps/onboarding-bff.md) | `it.pagopa.selfcare.onboarding.CucumberSuite` | `app/src/test/resources/docker-compose.yml` | `integration_tests_onboarding_bff.yml` (`mvn install -DskipTests` in `apps/onboarding-bff`, then tests in `apps/onboarding-bff/app`) |
| [institution-ms](./apps/institution-ms.md) | `it.pagopa.selfcare.mscore.integration_test.CucumberSuite` | `apps/institution-ms/docker-compose.yml` | `integration_test_institution_ms.yml` (from `apps/institution-ms`) |
| [webhook](./apps/webhook.md) | `it.pagopa.selfcare.webhook.cucumber.CucumberSuiteTest` | `apps/webhook/docker-compose.yml` | No dedicated workflow found |

Notes:

- The Quarkus runners for onboarding-ms and onboarding-functions are the step classes themselves
  (they extend `CucumberQuarkusTest`). The Spring runners use `@Suite` + `@IncludeEngines("cucumber")`
  with `@SpringBootTest`.
- Several runners open the compose file with a path relative to the working directory, so `iam`,
  `institution-ms`, `onboarding-bff/app` and `onboarding-functions` should be run from their own
  directory (`AGENTS.md`).
- `onboarding-functions`: the workflow downloads the Functions config from blob storage, runs
  `docker compose -f src/test/resources/docker-compose.yml up -d`, then runs the tests. The test image
  overrides configuration with `src/test/resources/fn/` (`application.properties`, `local.settings.json`,
  `host.json`, ...); mirror there any new property the tests need.
- If a run is interrupted, clean up with
  `docker compose -f <compose file> down -v --remove-orphans`.

The `integration_test(s)_*.yml` workflows run on pull requests to `main` and `releases/*` that touch the
app's folder (onboarding-ms also on `libs/selfcare-sdk-tenant/**`), and on `workflow_dispatch`, with
JDK 17 Temurin.

## Bruno contract tests (external-api)

The collection is in `apps/external-api/integration-test/bruno/` (folders `External`,
`InstitutionV2`, `UserV2`, `Internal`, `Support`, `Pnpg`; environment templates in `environments/`).
Unlike Cucumber, it runs against a **deployed environment through APIM**:

```bash
cd apps/external-api/integration-test/bruno
bru run . -r --env-file <environment>.json
```

In CI, `integration_test_external_api.yml` (push to `main` → dev, `releases/*` → uat, or manual
choice) calls `call_integration_test_bruno_external_api.yml`. That workflow decodes the
`INTEGRATION_ENVIRONMENT_BRUNO` secret into the environment file, installs `@usebruno/cli`, runs the
collection with a JSON reporter and posts the results. Never commit real subscription keys or tokens
in environment files. When an external API changes, update the matching Bruno request together with
the OpenAPI and APIM definition (see [external-api](./apps/external-api.md)).

## Coverage and SonarCloud

- `test-coverage/pom.xml` aggregates JaCoCo reports with one profile per module (apps, plus libs such
  as `selfcare-sdk-security`, `selfcare-sdk-health`, `selfcare-cucumber-sdk`, `onboarding-sdk`,
  `selfcare-user-sdk-*`) and a `report` profile:

  ```bash
  mvn --projects :test-coverage --also-make verify -P<module>,report
  ```

- `.github/workflows/pr_sonar_analysis.yml` runs on pull requests and pushes to `main` that touch
  `apps/**`, `libs/**`, `test-coverage/**` or `pom.xml`. It works out which projects changed, runs
  `mvn clean test --projects <changed> --also-make` (Java 17, Maven 3.9.5), collects every
  `target/**/jacoco.xml` and runs the Sonar scanner (Java 21) on the single project
  `pagopa_selfcare` (organization `pagopa`) with pull-request decoration.
- New behaviour needs tests that keep the SonarCloud quality gate green.

Related: [architecture.md](./architecture.md), [ci-cd.md](./ci-cd.md).
