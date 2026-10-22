# AGENTS.md

Guidance for AI coding agents (GitHub Copilot, Claude Code, Codex, etc.) working in this repository. `CLAUDE.md` and `.github/copilot-instructions.md` only point here: edit this file, not those.

## Overview

SelfCare monorepo (PagoPA): microservices, SDKs, infrastructure and release automation for the platform that manages identity, onboarding, products, institutions, users and webhooks for the Italian public sector.

- Two tenants/domains: **SELC** (Area Riservata, `ar`) and **PNPG** (Piattaforma Notifiche PG, `pnpg`)
- Environments: **DEV** (auto-deploy on `main`), **UAT** (auto-deploy on `releases/*`), **PROD** (manual approval)
- Java 17 (`maven.compiler.release`), Maven 3.9.x, Node/pnpm + Nx for workspace tooling

## Repository Layout

| Path | Content |
|------|---------|
| `apps/` | Deployable services (Maven modules, one `pom.xml` each) |
| `libs/` | Shared SDKs and parent POMs (`selfcare-sdk-pom`, `selfcare-sdk-security`, `selfcare-sdk-health`, `selfcare-cucumber-sdk`, `selfcare-onboarding-sdk-*`, `selfcare-user-sdk-*`, `selfcare-commons`, `selfcare-sdk-tenant*`, ...) |
| `test-coverage/` | Aggregated JaCoCo reports for SonarCloud (one profile per module) |
| `infra/` | Terraform (`core`, `resources`, `bootstrap`, `repository`, `cert`, `dashboards`, ...) |
| `doc/<app>/knowledge.md` | Per-app notes (present for some apps, e.g. `product`, `onboarding-*`): read before changing that app |
| `docs/` | Architecture diagrams and specs |
| `.github/workflows/` | CI/CD: `pr_<app>.yml` (PR checks), `pr_<app>_infra.yml`, `trigger_release_<app>.yml`, `release_*.yml`, `integration_test*.yml` |

Apps: `auth`, `iam`, `product`, `product-cdc`, `webhook`, `institution-ms`, `institution-cdc`, `institution-send-mail-scheduler`, `user-ms`, `user-cdc`, `user-group-ms`, `user-group-cdc`, `dashboard-bff`, `external-api`, `onboarding-ms`, `onboarding-bff`, `onboarding-cdc`, `onboarding-functions` (Azure Functions), `document-ms`, `delegation-cdc`, `registry-proxy`, `registry-proxy-runner`, `log-availability-runner`.

## Stack: check the app before assuming

Apps do **not** share one stack or version. Read the app's `pom.xml` first.

- **Quarkus apps** (most): `quarkus.platform.version` is declared per app (3.11.x – 3.36.x). Parent is `it.pagopa.selfcare:apps`.
- **Spring Boot apps** (`dashboard-bff`, `external-api`, `institution-ms`, `onboarding-bff`, `registry-proxy`, `user-group-ms`): parent `selc-starter-parent`; some are multi-module (`connector/`, `web/`, `core/`).
- Common libraries: MongoDB (Panache on Quarkus), MapStruct, Lombok, OpenAPI Generator, SmallRye (JWT, Health, OpenAPI), JUnit 5 + Mockito, Cucumber + Testcontainers.
- Most Quarkus services use SmallRye Mutiny (`Uni`/`Multi`) and reactive Panache; follow the style already used in the app you are editing (reactive vs blocking), do not mix.

## Build & Test

Prefer Nx when it covers the task (see Nx section below); plain Maven is the canonical fallback:

```bash
mvn clean package                                   # everything
mvn clean package -DskipTests
mvn -f apps/<app>/pom.xml clean package             # one app
mvn -f apps/<app>/pom.xml test                      # unit tests
mvn -f apps/<app>/pom.xml test -Dtest=MyServiceTest#myMethod
mvn -f apps/<app>/pom.xml verify -DskipITs=false    # Cucumber ITs (needs Docker)
mvn -f apps/<app>/pom.xml quarkus:dev               # Quarkus dev mode
mvn --projects :selfcare-cucumber-sdk --also-make-dependents clean package -DskipTests   # lib + dependents
mvn --projects :test-coverage --also-make verify -P<module>,report                       # coverage
mvn versions:set -DnewVersion=<x.y.z> --projects :<lib> && mvn versions:commit           # bump lib version
```

- Builds need credentials in `~/.m2/settings.xml` for Azure DevOps (`selfcare-platform`) and GitHub Packages (`pagopa/selfcare`).
- **Reproducible toolchain**: Maven 3.9.x (CI uses 3.9.5), Java 17 Temurin (as in CI), Node from `.node-version`, Terraform from `.terraform-version`.
- `skipITs` defaults to `true` in some app POMs: pass `-DskipITs=false` to actually run integration tests.
- Run only the module you changed (and dependents when touching `libs/`); the full build is slow.

## Coding Conventions

- Match the layered layout already used by the app, typically under `it.pagopa.selfcare.<app>/`: `controller/`, `service/` (interface + `Impl`), `repository/`, `entity/` (or `model/`), `dto/`, `mapper/`, `config/`, `exception/`.
- Controllers are secured (`@Authenticated` on Quarkus apps); JWT public key comes from config (`mp.jwt.verify.publickey`), never hard-code keys or secrets.
- MapStruct: `@Mapper(componentModel = "cdi", ...)` on Quarkus apps (use `ReportingPolicy.IGNORE` when the target has extra fields); include `mapstruct-lombok` when using Lombok.
- Error handling: a central exception handler per app (`@ServerExceptionMapper` on Quarkus), returning `Problem` / `application/problem+json`.
- OpenAPI: generated schema stored in `src/main/docs/`; operation IDs use method names. If you change an API, regenerate and commit the updated schema.
- Reuse the existing libraries in `libs/` before adding new dependencies.
- Formatting (`.editorconfig`): UTF-8, LF, 2-space indent, final newline, trim trailing whitespace.

## Testing

- Unit tests: JUnit 5 + Mockito; on Quarkus use `@QuarkusTest` with `@InjectMock` (not `@Mock`) for CDI beans. Reactive code: `UniAssertSubscriber` or `.await().indefinitely()`.
- Integration tests: Cucumber `.feature` files in `src/test/resources/features/`, `CucumberSuiteTest` + a `@TestProfile` (`IntegrationProfile`), Testcontainers `ComposeContainer` with `src/test/resources/docker-compose.yml`; helpers in `libs/selfcare-cucumber-sdk`.
- New behaviour needs tests; keep coverage in line with the SonarCloud quality gate (single project key `pagopa_selfcare`).

### Cucumber (BDD integration tests)

- Which apps have a suite: `auth`, `iam`, `product`, `document-ms`, `user-ms`, `user-group-ms`, `onboarding-ms`, `onboarding-functions`, `dashboard-bff`, `onboarding-bff`, `institution-ms`, `webhook`. Runner class is `CucumberSuite` / `CucumberSuiteTest` (names vary per app; look in `src/test/java`).
- Stack: the runner starts the app's `docker-compose.yml` (Mongo, Azurite, mock servers, sometimes other services) via Testcontainers; JWTs are generated by `selfcare-cucumber-sdk` with a test key pair, never real credentials.
- Many app POMs **exclude** the Cucumber runner from Surefire, so `mvn test` / `verify` do not run them. Select it explicitly: `mvn --projects :<app> --also-make test -Dtest=CucumberSuiteTest -Dsurefire.failIfNoSpecifiedTests=false`. Some apps must be run from their own directory (e.g. `iam`, `institution-ms`, `onboarding-bff/app`, `onboarding-functions`).
- `failIfNoSpecifiedTests=false` silently runs nothing if the class name is wrong: check that Surefire reports `Tests run: N` with N > 0. Report: `target/cucumber-report/cucumber.html`.
- Needs Docker. If a run is interrupted, clean up: `docker compose -f apps/<app>/src/test/resources/docker-compose.yml down -v --remove-orphans`.
- `onboarding-functions`: the test image overrides config with `src/test/resources/fn/`; new properties needed by the tests must be mirrored there.
- CI: `integration_tests_*.yml` per app (JDK 17, Maven 3.9.5).

### Bruno (external API contract tests)

- Collection in `apps/external-api/integration-test/bruno/` (folders `External`, `InstitutionV2`, `UserV2`, `Internal`, `Support`, `Pnpg`; environments in `environments/`).
- Unlike Cucumber, it runs against a **deployed environment through APIM** (dev/uat), not a local stack: `bru run . -r --env-file <environment>.json`. In CI (`call_integration_test_bruno_external_api.yml`, triggered by `integration_test_external_api.yml`) the environment file is decoded from the `INTEGRATION_ENVIRONMENT_BRUNO` secret, results are posted to Slack.
- Never commit real APIM subscription keys or tokens in Bruno environment files; use secret variables or the CI secret.
- When an external API changes (path, payload, status), update the matching Bruno request together with the OpenAPI/APIM definition.

## API exposure: OpenAPI and APIM

- Azure API Management (APIM) is the **only HTTP entrypoint** to the microservices (Container Apps): one instance per environment, internal VNet behind the Application Gateway, domain `api.<env>.selfcare.pagopa.it` (PNPG: `api-pnpg...`). Subscriptions/keys are not managed in Terraform.
- Two families of APIs, both defined in Terraform under `infra/resources/`:
  - **Application APIs** (BFFs, `auth`, `iam`, `webhook`, `registry-proxy`): module `_modules/apim_api`, one APIM API per app (base path e.g. `auth`, `dashboard`, `onboarding`; PNPG under `imprese/...`). APIM does not validate the user JWT: it forwards `Authorization` and the backend validates it.
  - **External APIs** for adhering products (`external-api` and the `*-ms`): module `_modules/apim_external_api/{api,api_pnpg}/<name>/<version>/`, with products and subscription keys; APIM mints an internal RS256 JWT from the subscription key.
- Multi-tenancy: the tenant (SELC = `AR`, PNPG = `PNPG`) travels in the `X-Tenant-Id` header and is enforced by APIM policies.
- The OpenAPI spec of each app is **committed** and is the source for APIM:
  - Quarkus: `apps/<app>/src/main/docs/openapi.{json,yaml}`, regenerated by the build (`mvn --projects :<app> --also-make package -DskipTests`).
  - Spring: generated by the `SwaggerConfigTest` test into `swagger/api-docs.json` or `src/main/docs/openapi.json` (`mvn -f apps/<app>/pom.xml test -Dtest=SwaggerConfigTest -Dsurefire.failIfNoSpecifiedTests=false`).
  - `apps/auth` also has a hand-maintained `openapi-apim.json` containing only the APIs published in APIM.
- Rules enforced by Optic (`optic.yml`): no breaking changes (create a new `v2` instead), `operationId` = Java method name, every operation has summary/description, `param-case` headers, `camelCase` query params. Run locally: `optic diff <spec> --base origin/main --check`.
- Clients for other services are generated from **committed copies** of their specs (`apps/<app>/src/main/openapi/` for Quarkus, `docs/openapi/` for Spring); update the copy in consumers when an API changes.
- Exposing or changing an API checklist: regenerate and commit the spec -> update the definition under `infra/resources/_modules/apim_external_api/...` (or `apim_api` usage) -> update the Bruno request -> PR (`pr_detect_openapi_*` workflows run Optic). APIM is released by `release_apim.yml` (push on `main` -> dev, `releases/**` -> uat, manual for prod).

## Infrastructure & CI

- Terraform lives in `infra/` (formatted/validated via `.pre-commit-config.yaml`); app-specific infra is released by the matching `*_infra.yml` workflows.
- Do not edit generated or release workflows without checking the matching `pr_*`/`trigger_release_*` pair.
- Never commit secrets, tokens or `settings.xml` content.

## Commits & PRs

- Recent history uses `[SELC-<n>] <type>(<scope>): <message>` (e.g. `[SELC-9497] institution-ms: ...`); follow `.github/PULL_REQUEST_TEMPLATE.md` for PRs.

<!-- nx configuration start-->
<!-- Leave the start & end comments to automatically receive updates. -->

# General Guidelines for working with Nx

- For navigating/exploring the workspace, invoke the `nx-workspace` skill first - it has patterns for querying projects, targets, and dependencies
- When running tasks (for example build, lint, test, e2e, etc.), always prefer running the task through `nx` (i.e. `nx run`, `nx run-many`, `nx affected`) instead of using the underlying tooling directly
- Prefix nx commands with the workspace's package manager (e.g., `pnpm nx build`, `npm exec nx test`) - avoids using globally installed CLI
- You have access to the Nx MCP server and its tools, use them to help the user
- For Nx plugin best practices, check `node_modules/@nx/<plugin>/PLUGIN.md`. Not all plugins have this file - proceed without it if unavailable.
- NEVER guess CLI flags - always check nx_docs or `--help` first when unsure

## Scaffolding & Generators

- For scaffolding tasks (creating apps, libs, project structure, setup), ALWAYS invoke the `nx-generate` skill FIRST before exploring or calling MCP tools

## When to use nx_docs

- USE for: advanced config options, unfamiliar flags, migration guides, plugin configuration, edge cases
- DON'T USE for: basic generator syntax (`nx g @nx/react:app`), standard commands, things you already know
- The `nx-generate` skill handles generator discovery internally - don't call nx_docs just to look up generator syntax


<!-- nx configuration end-->