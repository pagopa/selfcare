# SelfCare documentation

Documentation of the SelfCare monorepo (PagoPA): the microservices, SDKs, infrastructure and release
automation that manage identity, onboarding, products, institutions, users and webhooks for the Italian
public sector.

Two tenants share the platform:

| Tenant | Id | Domain | Authentication |
|---|---|---|---|
| SelfCare Area Riservata | `AR` | `selfcare.pagopa.it` | OneIdentity, token exchange in [`auth`](apps/auth.md) |
| Piattaforma Notifiche Persone Giuridiche | `PNPG` | `imprese.notifichedigitali.it` | `hub-spid-login` |

Environments: **DEV** (auto-deploy from `main`), **UAT** (auto-deploy from `releases/*`), **PROD** (manual approval).

> Contributor and AI-agent rules (build commands, coding conventions, commit format) live in
> [`AGENTS.md`](../AGENTS.md). This folder describes *what* the system is and *how* it works.

## Start here

| Page | Content |
|---|---|
| [Architecture](architecture.md) | System context, service catalogue, main request and event flows |
| [Multitenancy](multitenancy.md) | Tenant model, Step 0-2 roadmap and implementation status |
| [Infrastructure](infrastructure.md) | Terraform layout, Application Gateway, APIM, Container Apps, data stores |
| [CI/CD and releases](ci-cd.md) | GitHub workflows, PR checks, release branches, deployments |
| [Testing](testing.md) | Unit tests, Cucumber suites, Bruno contract tests, coverage |
| [Specifications](specs/README.md) | Index of requirements, architecture and security specs |

## Applications (`apps/`)

| Domain | Services |
|---|---|
| Identity and access | [auth](apps/auth.md), [iam](apps/iam.md) |
| Product catalogue | [product](apps/product.md), [product-cdc](apps/product-cdc.md) |
| Onboarding | [onboarding-ms](apps/onboarding-ms.md), [onboarding-bff](apps/onboarding-bff.md), [onboarding-functions](apps/onboarding-functions.md), [onboarding-cdc](apps/onboarding-cdc.md), [document-ms](apps/document-ms.md) |
| Institutions | [institution-ms](apps/institution-ms.md), [institution-cdc](apps/institution-cdc.md), [institution-send-mail-scheduler](apps/institution-send-mail-scheduler.md), [delegation-cdc](apps/delegation-cdc.md) |
| Users and groups | [user-ms](apps/user-ms.md), [user-cdc](apps/user-cdc.md), [user-group-ms](apps/user-group-ms.md), [user-group-cdc](apps/user-group-cdc.md) |
| Dashboard | [dashboard-bff](apps/dashboard-bff.md) |
| External integrations | [external-api](apps/external-api.md), [webhook](apps/webhook.md), [webhook-consumer](apps/webhook-consumer.md) |
| Registries | [registry-proxy](apps/registry-proxy.md), [registry-proxy-runner](apps/registry-proxy-runner.md) |
| Operations | [log-availability-runner](apps/log-availability-runner.md) |

## Libraries (`libs/`)

| Area | Libraries |
|---|---|
| Parent POMs | [selfcare-sdk-pom](libs/selfcare-sdk-pom.md), [selfcare-starter-parent](libs/selfcare-starter-parent.md), [selfcare-onboarding-sdk-pom](libs/selfcare-onboarding-sdk-pom.md), [selfcare-user-sdk-pom](libs/selfcare-user-sdk-pom.md) |
| Security and tenancy | [selfcare-sdk-security](libs/selfcare-sdk-security.md), [selfcare-sdk-tenant](libs/selfcare-sdk-tenant.md), [selfcare-sdk-tenant-mongodb](libs/selfcare-sdk-tenant-mongodb.md), [selfcare-sdk-m2m](libs/selfcare-sdk-m2m.md) |
| Spring commons | [selfcare-commons](libs/selfcare-commons.md) |
| Health | [selfcare-sdk-health](libs/selfcare-sdk-health.md), [selfcare-sdk-health-spring](libs/selfcare-sdk-health-spring.md) |
| Onboarding SDK | [selfcare-onboarding-sdk-common](libs/selfcare-onboarding-sdk-common.md), [selfcare-onboarding-sdk-product](libs/selfcare-onboarding-sdk-product.md), [selfcare-onboarding-sdk-crypto](libs/selfcare-onboarding-sdk-crypto.md), [selfcare-onboarding-sdk-azure-storage](libs/selfcare-onboarding-sdk-azure-storage.md) |
| User SDK | [selfcare-user-sdk-model](libs/selfcare-user-sdk-model.md), [selfcare-user-sdk-event](libs/selfcare-user-sdk-event.md) |
| Events | [selfcare-internal-events-sdk](libs/selfcare-internal-events-sdk.md) |
| Testing | [selfcare-cucumber-sdk](libs/selfcare-cucumber-sdk.md) |

## Other sources

- Architecture diagram: [selfcare-architecture.png](architectures/selfcare-architecture.png)
  (editable source: [selfcare-architecture.drawio](architectures/selfcare-architecture.drawio)).
- Per-app `README.md` files under `apps/<app>/` (linked from each app page).
- OpenAPI specs: committed per app (`apps/<app>/src/main/docs/` for Quarkus, `swagger/` or
  `src/main/docs/` for Spring); see [Architecture](architecture.md) and [Infrastructure](infrastructure.md).
- Confluence space SCP (design records and RFCs), e.g.
  [RFC - Multitenancy Strategia Unico Backend](https://pagopa.atlassian.net/wiki/spaces/SCP/pages/3456303148).

## Maintaining this documentation

- Update the app or lib page in the same PR that changes its API, configuration, data model or deployment.
- New specs go in `docs/specs/<FEATURE>/` (`REQUIREMENTS.md`, `ARCHITECTURE.md`, `SECURITY.md`, optional
  `EPIC.md`) and must be added to the [specs index](specs/README.md).
- Keep links relative and cite repository paths for facts, so pages can be checked against the code.
