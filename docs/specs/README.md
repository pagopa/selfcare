# Specifications index

This folder holds the design specs for cross-cutting SelfCare initiatives. Each spec lives in its own
folder. Specs describe intent and design: always check the code for what is actually implemented
(for multitenancy, see [../multitenancy.md](../multitenancy.md)).

## File convention

Most spec folders use the same set of files:

| File | Role |
|------|------|
| `REQUIREMENTS.md` | Source of truth. Numbered requirements (`SELC-x` / `SELC-x.y`) with acceptance criteria. |
| `ARCHITECTURE.md` | Design that satisfies the requirements: "Required Architecture Inputs", "Initial Architecture (Provisional)", "Requirement Traceability", "Dependency Rules". |
| `SECURITY.md` | "Required Security Inputs", "Provisional Security Rules" (each citing its source, often OWASP guidance when no prompt library exists) and "Selected Prompts". |
| `EPIC.md` | Jira epic and story breakdown derived from the requirements (only some folders have one). |

Extra files in a folder (for example the `Step_2/*_identification.md` notes) go deeper into one topic
of the same initiative.

## FN_VPN: private onboarding Function App

| File | Summary |
|------|---------|
| [REQUIREMENTS.md](./FN_VPN/REQUIREMENTS.md) | Make the onboarding Function App reachable only through a private endpoint and the VPN without breaking existing callers. |
| [ARCHITECTURE.md](./FN_VPN/ARCHITECTURE.md) | Private endpoint, DNS and access-path design for `selc-<env>[-pnpg]-onboarding-fn` (`infra/resources/_modules/functions`). |
| [SECURITY.md](./FN_VPN/SECURITY.md) | Network exposure rules and handling of function keys (Key Vault secret `fn-onboarding-primary-key`). |

## LOG_AVAILABILITY_DAILY: daily API availability

| File | Summary |
|------|---------|
| [REQUIREMENTS.md](./LOG_AVAILABILITY_DAILY/REQUIREMENTS.md) | Nightly D-1 API availability computed from App Gateway access logs (status < 300 or 404 counts as available). |
| [ARCHITECTURE.md](./LOG_AVAILABILITY_DAILY/ARCHITECTURE.md) | Quarkus command-mode Container Apps Job writing to Table Storage, with a dashboard and Slack alerts. This is [log-availability-runner](../apps/log-availability-runner.md). |
| [SECURITY.md](./LOG_AVAILABILITY_DAILY/SECURITY.md) | Identity, data access and secret rules for the job. |

## ONBOARDING_ADMISSION_RULES: product admission rules

| File | Summary |
|------|---------|
| [REQUIREMENTS.md](./ONBOARDING_ADMISSION_RULES/REQUIREMENTS.md) | product-ms becomes the single source of truth for which `(institutionType, origin)` pairs can onboard, and with which workflow (Jira SELC-9538). |
| [ARCHITECTURE.md](./ONBOARDING_ADMISSION_RULES/ARCHITECTURE.md) | Replaces the dual use of `institutionOrigins` / `InstitutionTypeValidator`; split into product (SELC-9539) and onboarding (SELC-9540) work. |

This folder has no `SECURITY.md`.

## MULTITENANT: one backend for AR and PNPG

Summary of the whole initiative and its implementation status: [../multitenancy.md](../multitenancy.md).

| File | Summary |
|------|---------|
| [DESIGN_REVIEW.md](./MULTITENANT/DESIGN_REVIEW.md) | Design review in Italian: needs, actors, use cases, C4 diagrams, data layer (`tenant_data_isolation` registry), Mongo dispatch for Quarkus and Spring, storage, BC/DR. |

### Step 0: tenant identity

| File | Summary |
|------|---------|
| [REQUIREMENTS.md](./MULTITENANT/Step_0/REQUIREMENTS.md) | SELC-1..7: `X-Tenant-Id` header, `tenant_id` JWT claim, claim-less hub-spid-login tokens default to PNPG, APIM resolves the tenant, shared resolution strategy, per-app refactor. |
| [ARCHITECTURE.md](./MULTITENANT/Step_0/ARCHITECTURE.md) | How the tenant is resolved at APIM and propagated to and validated by the services. |
| [SECURITY.md](./MULTITENANT/Step_0/SECURITY.md) | APIM resolves the tenant from the Host header and strips any client-supplied `X-Tenant-Id`. |
| [EPIC.md](./MULTITENANT/Step_0/EPIC.md) | Jira epic breakdown for Step 0. |
| [SELC-9170/REQUIREMENTS.md](./MULTITENANT/Step_0/SELC-9170/REQUIREMENTS.md) | Remaining auth work: per-tenant session signing keys and `kid`, key validation at startup, tenant claim in dashboard-bff exchange tokens. |
| [SELC-9170/ARCHITECTURE.md](./MULTITENANT/Step_0/SELC-9170/ARCHITECTURE.md) | Design for per-tenant signing and verification-key selection (tenant from the URL, plus `kid`). |
| [SELC-9170/SECURITY.md](./MULTITENANT/Step_0/SELC-9170/SECURITY.md) | Key-handling rules for SELC-9170. |

### Step 1: data isolation

| File | Summary |
|------|---------|
| [REQUIREMENTS.md](./MULTITENANT/Step_1/REQUIREMENTS.md) | SELC-8 (Cosmos discriminator or database per tenant), SELC-9 (Storage container or account per tenant), SELC-10 (personal data vault), SELC-11 (email sender domain). |
| [ARCHITECTURE.md](./MULTITENANT/Step_1/ARCHITECTURE.md) | Isolation design for Cosmos DB, Storage, PDV and email. |
| [SECURITY.md](./MULTITENANT/Step_1/SECURITY.md) | Fail-closed and isolation rules for Step 1. |
| [EPIC.md](./MULTITENANT/Step_1/EPIC.md) | Jira epic breakdown for Step 1. |
| [scripts/README.md](./MULTITENANT/Step_1/scripts/README.md) | Runbook for backfilling `tenantId` on legacy documents. Dry run by default; never reassigns a tenant. |
| [scripts/backfill_tenant_id.py](./MULTITENANT/Step_1/scripts/backfill_tenant_id.py) | The backfill script (`--tenant`, `--account-name`, `--apply`, `--batch-size`, `--verify`, `--allow-missing`). |
| [scripts/test_backfill_tenant_id.py](./MULTITENANT/Step_1/scripts/test_backfill_tenant_id.py) | Unit tests for the backfill script. |

### Step 2: strict isolation and single ingress

| File | Summary |
|------|---------|
| [REQUIREMENTS.md](./MULTITENANT/Step_2/REQUIREMENTS.md) | SELC-12..19: canonical runtime tenant, strict Cosmos isolation, storage bindings, PDV, email, shared vs dedicated config, cutover gates, single ingress. Includes a resource routing matrix. |
| [ARCHITECTURE.md](./MULTITENANT/Step_2/ARCHITECTURE.md) | Trusted `TenantContext`, tenant resource registry, Mongo and storage routing, consolidation boundary. |
| [SECURITY.md](./MULTITENANT/Step_2/SECURITY.md) | Security rules for Step 2. |
| [Auth_implementation.md](./MULTITENANT/Step_2/Auth_implementation.md) | Plan for auth: `TenantRegistry`, OneIdentity credentials and user-registry key per tenant, AR only for now (PNPG `auth_enabled=false`). |
| [DocumentMs_implementation.md](./MULTITENANT/Step_2/DocumentMs_implementation.md) | Plan for document-ms, in Italian, with SELC-DMS-NN stories. |
| [Database_identification.md](./MULTITENANT/Step_2/Database_identification.md) | Mongo routing contract: JWT / `X-Tenant-Id` → `TenantContext` → registry → client and database. |
| [Storage_identification.md](./MULTITENANT/Step_2/Storage_identification.md) | Storage selected by tenant plus a logical key, e.g. the several bindings used by onboarding-ms. |
| [JWT_Key_Resolution.md](./MULTITENANT/Step_2/JWT_Key_Resolution.md) | Verification key taken from the registry `jwt.publicKeyEnvVar` instead of the flat `mp.jwt.verify.publickey`. |
| [Ingress_unification.md](./MULTITENANT/Step_2/Ingress_unification.md) | One App Gateway listener and one APIM API per BFF, the tenant trust order, the rollout plan and open decisions. |

## OpenSpec change in institution-ms

`apps/institution-ms/openspec/` uses the OpenSpec layout (`config.yaml` with schema `spec-driven`,
`changes/`, `specs/`).

| File | Summary |
|------|---------|
| [config.yaml](../../apps/institution-ms/openspec/config.yaml) | OpenSpec configuration for institution-ms. |
| [proposal.md](../../apps/institution-ms/openspec/changes/institution-ms-multitenant-database-layer/proposal.md) | Why and what: adopt `selc-commons-tenant` and add `tenantId` to Institution, Delegations and MailNotification. |
| [design.md](../../apps/institution-ms/openspec/changes/institution-ms-multitenant-database-layer/design.md) | Design: tenant registry (`SELFCARE_TENANT_DATA_ISOLATION`) and strict mode (`TENANT_STRICT_DATA_ISOLATION`, default `true`). |
| [tasks.md](../../apps/institution-ms/openspec/changes/institution-ms-multitenant-database-layer/tasks.md) | Implementation checklist (all tasks checked). |
| [spec.md](../../apps/institution-ms/openspec/changes/institution-ms-multitenant-database-layer/specs/institution-tenant-persistence/spec.md) | Requirements for tenant-aware institution persistence. |

## Other documents

| File | Summary |
|------|---------|
| [selfcare-architecture.png](../architectures/selfcare-architecture.png) | High-level SelfCare architecture diagram, described in [../architecture.md](../architecture.md). |
| [selfcare-architecture.drawio](../architectures/selfcare-architecture.drawio) | Editable draw.io source of the diagram. |
| [selfcare-architecture.drawio.png](../architectures/selfcare-architecture.drawio.png) | PNG export of the draw.io source. |
| [Quarkus_banner.md](../Quarkus_banner.md) | How to show the Maven project version in the Quarkus startup banner through resource filtering. |
