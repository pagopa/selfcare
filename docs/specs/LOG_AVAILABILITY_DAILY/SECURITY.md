# LOG_AVAILABILITY_DAILY — Security

Sources: [REQUIREMENTS.md](./REQUIREMENTS.md) (`SELC-x.y`) and [ARCHITECTURE.md](./ARCHITECTURE.md) (components C1–C7, gaps G1–G5).

Prompt library: not available. No `PROMPT.md` library was found in the repo, the local workspace, GitHub code search or Copilot Spaces. Each area therefore falls back to (a) the repo's own patterns under `infra/core/*`, `infra/resources/*`, `apps/*` and `.github/workflows/*`, and (b) public OWASP sources:
- Proactive Controls (**OPC**)
- Cheat Sheet Series (**CS**)
- API Security Top 10 (**API10**)

Threat profile from ARCHITECTURE.md: this is **not** a web/API application. It is an outbound-only nightly batch job (C2) with no inbound HTTP surface, no user sessions and no custom UI.
- Trust boundaries: Managed Identity → Log Analytics (read), Table Storage and Logs Ingestion API (write), Azure Monitor → Slack (notify), Entra ID users → Azure Portal Dashboard (read).
- Priorities are adjusted accordingly: identity and authorization, secret handling, input validation of job parameters and the KQL they feed, logging and error handling, and IaC / CI/CD safety. HTTP boundary controls (CORS, CSP, JWT, rate limiting, webhooks, APIM) are out of scope.

## Required Security Inputs

- Runtime environment: Azure Container Apps Job (`infra/resources/_modules/container_app_job`).
- Server framework: Quarkus 3.31.x / Java 17 command mode; no inbound HTTP surface.
- Inbound surface: none. Triggers are schedule and manual (backfill / on-demand). Who may start a manual run: TO BE DECIDED.
- Job parameters (environment, reference date): source and allowed range TO BE DECIDED (SELC-5.2–5.4).
- Workload identity: system-assigned Managed Identity (SELC-4.7).
- RBAC scopes for the job and for dashboard viewers: TO BE DECIDED (ARCHITECTURE.md C7).
- Source logs workspace and access mode (resource-context vs workspace): UNKNOWN (G1).
- Network path to Table storage (private endpoint + DNS): TO BE DECIDED (G3).
- Slack routing (action group ↔ channel): UNKNOWN (G5).
- Data classification: aggregated counts only, no personal data (SELC-4.4); no regulatory constraints.

## Provisional Security Rules

Safe, durable defaults grounded in known facts. Each rule cites its source so the full guidance can be loaded on demand.

### 1. Workload identity and secrets

- **SEC-1.1** Authenticate every outbound call (Log Analytics query, Table Storage, Logs Ingestion) with an Entra ID token for the Managed Identity. Never use storage account keys, SAS, connection strings or the DCR shared-key/HTTP Data Collector API (SELC-4.7).
  - Sources: OWASP CS *Secrets Management*; `infra/resources/_modules/container_app_job/container_app_job.tf` (`identity` block).
- **SEC-1.2** The job MUST NOT read the existing Key Vault secrets `logs-storage-access-key`, `logs-storage-connection-string` or `logs-storage-blob-connection-string`, created by `infra/core/_modules/storage_account_template/main.tf`. Do not grant it Key Vault access it does not need.
  - Source: OWASP OPC C1 *Implement Access Control* (least privilege).
- **SEC-1.3** Any secret the job does need comes from Key Vault through a Container App secret reference resolved with an identity (`key_vault_secret_id` + `identity`). Never use plain env values, Terraform variables or code.
  - Sources: `infra/resources/_modules/container_app_job/container_app_job.tf` (`secret` block); OWASP CS *Secrets Management*.
- **SEC-1.4** Slack target addresses stay in Key Vault and are referenced by action groups. They are never written into code, Terraform literals or logs.
  - Source: `infra/core/_modules/monitor/main.tf` (action groups reading `azurerm_key_vault_secret`).

### 2. Authorization (Azure RBAC)

- **SEC-2.1** Grant the job identity only the following roles:
  - Read on the App Gateway source logs: `Log Analytics Reader`, or `Reader` on the gateway for resource-context queries. Pick one once G1 is resolved.
  - `Storage Table Data Contributor` scoped to the `SelcAvailability` table, or at most the logs storage account.
  - `Monitoring Metrics Publisher` scoped to the DCR only.
  - No `Contributor`/`Owner`, and no data-plane roles on other storage accounts.
  - Sources: `infra/core/_modules/user_managed_identity/user_managed_identity.tf` (precedent: dedicated identity + data-plane role per storage); OWASP CS *Authorization*.
- **SEC-2.2** Viewers (subscription users, SELC-6.8) get read-only access: dashboard read plus read on `SelcAvailability_CL`. Prefer table-level read over whole-workspace read. Viewers MUST NOT get any write role on `SelcAvailability`, the DCR or the job.
  - Source: OWASP CS *Authorization* (deny by default).
- **SEC-2.3** Starting a manual or backfill run is a privileged action. Restrict it to operators. The mechanism is TO BE DECIDED.
  - Source: OWASP OPC C1.
- **SEC-2.4** Declare all role assignments in Terraform. Do not create them by hand in the portal.
  - Source: OWASP CS *Infrastructure as Code Security*.

### 3. Input validation and query safety

- **SEC-3.1** Validate job parameters strictly before use:
  - Environment: allow-list `DEV|UAT|PROD`, which must match the running stack.
  - Reference date: parse as `yyyy-MM-dd` into a date type; reject today or future dates and dates outside retention (SELC-5.3, 5.4).
  - Reject everything else and fail without writing (SELC-4.5).
  - Source: OWASP CS *Input Validation*.
- **SEC-3.2** Build KQL only from constants and validated, typed values (e.g. `datetime(<ISO-8601 from parsed date>)`). Never concatenate raw parameter strings into the query. Keep the listener allow-list (`api`, `api-pnpg`) and the URI exclusions (`/spid/v1/metadata`, `dummy`) as code constants (SELC-2.6, 2.7).
  - Source: OWASP CS *Injection Prevention*.
- **SEC-3.3** Treat query results as untrusted: check types, non-negative counts, `total = count_lt_500 + count_gte_500`, and availability in `[0, 100]` before writing. Any mismatch fails the run.
  - Source: OWASP OPC C5 *Validate All Inputs*.

### 4. Data protection

- **SEC-4.1** Persist only the record fields in SELC-4.2. Never copy client IP, URI, query string, headers or user identifiers from `AzureDiagnostics` into the tables, logs or Slack messages (SELC-4.4).
  - Source: OWASP CS *User Privacy Protection*.
- **SEC-4.2** Reach Table storage only over the private network. Keep `public_network_access_enabled = false` on `selc{d,u,p}weusynthmon`; use its existing `table` private endpoint and `privatelink.table.core.windows.net` resolution rather than opening the account (G3).
  - Sources: `infra/core/_modules/synthetic_monitoring_storage/main.tf`.
- **SEC-4.3** Keep the synthetic monitoring account's `CanNotDelete` management lock (`azurerm_management_lock.this`). Scope the job's `Storage Table Data Contributor` role to the `SelcAvailability` table, not the account. `SelcAvailability` has no expiry (SELC-4.6).

### 5. Logging, errors and alerting

- **SEC-5.1** Never log tokens, secrets, Authorization headers or full SDK request dumps. Log run start and end, environment, reference date, counts and outcome.
  - Source: OWASP CS *Logging*.
- **SEC-5.2** On failure, exit non-zero with a generic error class. Slack notifications carry only environment, reference date and a failure reason without stack traces or secrets (SELC-5.1).
  - Source: OWASP CS *Error Handling*.
- **SEC-5.3** Fail closed on query and validation errors: do not write either destination and never write default or zero counts on error. If one destination write succeeds before the other fails, mark the run failed and rely on a retry to reconcile the reporting projection (SELC-4.5, 4.9).
  - Source: OWASP OPC C10 *Handle All Errors and Exceptions*.

### 6. Infrastructure as Code

- **SEC-6.1** Pin external Terraform modules to a tag or commit (repo precedent: `github.com/pagopa/terraform-azurerm-v4.git//...?ref=v9.6.1`).
- **SEC-6.2** Every `tfsec:ignore` needs an inline justification. Do not add new ignores for storage network rules.
- **SEC-6.3** No secrets in `.tf` / `.tfvars`. Read them from Key Vault data sources.
- **SEC-6.4** Keep the PROD-only resources and the per-environment stacks (`infra/core/{dev,uat,prod}-ar`) separate.
- Sources: `infra/core/*`, `infra/resources/*`; OWASP CS *Infrastructure as Code Security*.

### 7. CI/CD and supply chain

- **SEC-7.1** Use GitHub OIDC (`id-token: write` + `pagopa/dx/actions/csp-login`) for Azure. No long-lived cloud credentials in GitHub secrets. Use minimal workflow `permissions`.
- **SEC-7.2** Pin third-party actions by commit SHA (repo precedent). Treat `pagopa/dx/...@main` references as a known exception, not a pattern to extend.
- **SEC-7.3** Keep separate `-ci` (plan) and `-cd` (apply) GitHub environments, with approval for PROD.
- Sources: `.github/workflows/call_release_infra.yml`, `.github/workflows/call_release_docker.yml`, `.github/workflows/pr_institution_send_mail_scheduler_infra.yml`; OWASP CS *CI/CD Security*.
- **SEC-7.4** New dependencies follow ARCHITECTURE.md *Dependency Rules*: zero by default, latest stable, no known CVEs, transitive audit, pinned with a lockfile.
  - Source: OWASP CS *Vulnerable Dependency Management*.
- **SEC-7.5** If a container image is adopted (runtime still TO BE DECIDED), follow the repo precedent:
  - Base images pinned by digest, multi-stage build, non-root `USER`.
  - Images published only to `ghcr.io/pagopa`.
  - Pass registry credentials via BuildKit `--secret`, not `ARG`.
  - Sources: `apps/institution-send-mail-scheduler/Dockerfile`; OWASP CS *Docker Security*.

### 8. Code quality baseline

- **SEC-8.1** Follow the repo build gates: Sonar plus Jacoco coverage (`pom.xml`). Unit-test the validation (SEC-3), the classification and rounding (SELC-2, 3) and fail-closed paths (SEC-5.3).
  - Sources: `apps/*`, `pom.xml`; OWASP OPC C10.

## Selected Prompts

- `Code quality -> /apps/* (repo baseline: pom.xml Sonar/Jacoco) + OWASP Proactive Controls (fallback: no library prompt)`
- `API security / HTTP boundary -> Not applicable (no inbound API; ARCHITECTURE.md)`
- `Backend framework -> Quarkus: apps/log-availability-runner (Java 17 command-mode job)`
- `Runtime / container -> Azure Container Apps Job: infra/resources/_modules/container_app_job`
- `Client framework -> Azure Monitor Workbook: infra/core/_modules/monitor`
- `Authentication (workload) -> Entra ID Managed Identity: infra/resources/_modules/container_app_job + OWASP CS Secrets Management (fallback)`
- `Authentication (users) -> Not applicable at app level (Azure Portal / Entra ID; no sessions, passwords, MFA or SSO handled by the system)`
- `Authorization -> Azure RBAC: infra/core/_modules/user_managed_identity + OWASP CS Authorization (fallback)`
- `Input validation / injection (KQL) -> OWASP CS Input Validation + Injection Prevention (fallback: no library prompt)`
- `Secret management -> Key Vault: infra/core/_modules/key_vault, infra/core/_modules/monitor + OWASP CS Secrets Management (fallback)`
- `Network / data protection -> infra/core/_modules/synthetic_monitoring_storage + OWASP CS User Privacy Protection (fallback)`
- `Logging and error handling -> infra/core/_modules/monitor (action groups) + OWASP CS Logging, Error Handling (fallback)`
- `Infrastructure as Code -> /infra/core/*, /infra/resources/* + OWASP CS Infrastructure as Code Security (fallback)`
- `CI/CD -> .github/workflows/* + OWASP CS CI/CD Security (fallback)`
- `Dependencies / supply chain -> ARCHITECTURE.md Dependency Rules + OWASP CS Vulnerable Dependency Management (fallback)`
- `WAF -> Not applicable (App Gateway WAF_v2 is a log source only; not modified)`
- `API Management, OAuth2/OIDC for clients, JWT, CORS, CSP, rate limiting, file upload, SSRF, webhooks, OpenAPI validation -> Not applicable (no inbound HTTP surface)`
