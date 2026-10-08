# LOG_AVAILABILITY_DAILY — Architecture

Primary source of truth: [REQUIREMENTS.md](./REQUIREMENTS.md). Requirement IDs (`SELC-x.y`) refer to that file. Infrastructure facts come from `infra/core/*` and `infra/resources/*`.

## Required Architecture Inputs

- Requirements source: REQUIREMENTS.md
- System purpose: Each night, compute the previous UTC day's API availability for each environment (DEV, UAT, PROD) from Application Gateway access logs (`< 500` available vs `>= 500`/invalid unavailable). Store one record per day and report it on an Azure Portal Dashboard over any date range.
- Primary use cases: (1) scheduled D-1 computation and persistence (SELC-1, 2, 3, 4); (2) go-live backfill and on-demand regeneration of a past date (SELC-5.2–5.4); (3) failure notification (SELC-5.1); (4) range reporting: pie chart, daily column charts, totals, on-demand table (SELC-6).
- Target users / actors: Scheduled job (system actor, Managed Identity). Operators receiving Slack failure notifications. Report consumers: users of the environment's Azure subscription (SELC-6.8).
- Runtime environment: TO BE DECIDED. Repo precedent for scheduled batch work: Azure Container Apps Job in the environment's Container Apps Environment (e.g. `selc-p-cae-002`), using module `infra/resources/_modules/container_app_job` with `schedule_trigger_config` and `manual_trigger_config` and a `ghcr.io/pagopa/*` image (see `institution-send-mail-scheduler`, `registry-proxy-runner`). Container Apps cron expressions are evaluated in UTC.
- Server framework: TO BE DECIDED. Repo precedent: Quarkus 3.31.x / Java 17 command-mode application (`@QuarkusMain`, as `apps/institution-send-mail-scheduler`). No inbound HTTP server is required.
- Client framework: None (no custom client). The UI is a new Azure Portal Dashboard (`azurerm_portal_dashboard`, JSON template rendered with `templatefile`, same pattern as `monitoring-dashboard` in `infra/core/_modules/monitor`) with Log Analytics (KQL) tiles (SELC-6.3).
- API style and integration model: No inbound API. Outbound-only batch over HTTPS with Entra ID tokens:
  - Log Analytics query (KQL over `AzureDiagnostics`, App Gateway access logs).
  - Azure Table Storage upsert into `SelcAvailability`.
  - Azure Monitor Logs Ingestion API through a Data Collection Rule into `SelcAvailability_CL`. This is the only Managed-Identity path into a custom table; the legacy HTTP Data Collector API needs a shared key, which SELC-4.7 forbids.
  - Failure notification through Azure Monitor alert → action group → email-to-Slack receiver (existing pattern in `infra/core/_modules/monitor`).
- Authentication and session model: No user sessions and no application-level login.
  - Writer: Entra ID Managed Identity (Container Apps Jobs already get `SystemAssigned, UserAssigned`). Keys, SAS and connection strings MUST NOT be used for writes (SELC-4.7).
  - Readers: Entra ID users of the subscription through the Azure Portal. Dashboard tiles query with the viewer's identity (SELC-6.8).
- Data model expectations: One Daily Availability Record per (reference date, environment): reference date, environment, `count_lt_500`, `count_gte_500`, total, availability (2 decimal places), generation timestamp (SELC-4.2, 3.3).
  - `SelcAvailability` (system of record, no expiry): `PartitionKey = yyyy`, `RowKey = yyyy-MM-dd` (UTC); upsert replaces the record.
  - `SelcAvailability_CL` (reporting copy, append-only, 730-day Analytics retention): mandatory `TimeGenerated` plus an explicit reference-date column. The latest entry per key is selected with `arg_max(generation timestamp)` (SELC-4.10).
  - Column names and types: TO BE DECIDED.
- Deployment model: Terraform, one stack per environment: `infra/core/{dev,uat,prod}-ar`, and a new `infra/resources/<app>/{dev,uat,prod}-ar` for the job if the Container Apps Job precedent is adopted. PNPG stacks (`*-pnpg`) are not involved: PNPG traffic is in the `-ar` gateway (`api-pnpg` listener).
  - New resources: the `SelcAvailability` table in `selc{d,u,p}stlogs`; a Table private endpoint; the `SelcAvailability_CL` table and its DCR; the dashboard; the failure alert; role assignments.
  - Exact module placement: TO BE DECIDED.
- Scale expectations: Very low write volume: 1 scheduled execution per day per environment (plus idempotent reruns) and 1 record per day per environment (~365 per year).
  - Read side: one 24-hour KQL aggregation over App Gateway access logs. Daily log volume: UNKNOWN. The gateway is WAF_v2 with autoscale 1–5 in PROD.
  - Backfill: one-off, bounded by the source log retention.
  - No latency target beyond completing the nightly run.
- Security expectations:
  - Least-privilege RBAC for the Managed Identity: read the source logs; `Storage Table Data Contributor` on the logs storage account; `Monitoring Metrics Publisher` on the DCR. Exact scopes: TO BE DECIDED.
  - Private network access to Table storage: the logs account has public network access disabled.
  - No personal data persisted (SELC-4.4).
  - The existing Key Vault secrets `logs-storage-access-key` / `logs-storage-connection-string` MUST NOT be used by the job.
  - Viewers need read access on `SelcAvailability_CL` (SELC-6.8).
  - Slack target addresses stay in Key Vault (existing pattern).
- AI / agent components: none.

## Initial Architecture (Provisional)

### Components

- **C1 Source logs.** App Gateway `${prefix}-app-gw` access logs in Log Analytics (`AzureDiagnostics`, Category `ApplicationGatewayAccessLog`; fields `httpStatus_d`, `requestUri_s`, listener).
  - Assumption: query them resource-context on the gateway, as the existing dashboard does, so the job does not depend on which workspace holds them (see G1).
- **C2 Availability job.** A stateless batch process, one instance per environment.
  - Input: environment, plus a reference date (default D-1 UTC, overridable for backfill or on-demand runs).
  - Steps: run one KQL aggregation over `[D-1 00:00Z, D 00:00Z)` restricted to listeners `api` and `api-pnpg`, excluding `/spid/v1/metadata` and `dummy`. Classify statuses (valid `100–499` → `count_lt_500`; anything else, including missing or invalid → `count_gte_500`), compute availability, write C3, then write C4.
  - Exit status: non-zero if any step fails. Nothing is written if the query fails (SELC-4.5).
- **C3 System of record.** Table `SelcAvailability` in `selcdstlogs` / `selcustlogs` / `selcpstlogs`. Upsert on (`PartitionKey`, `RowKey`). No lifecycle deletion.
- **C4 Reporting copy.** Custom table `SelcAvailability_CL` in the environment workspace (`selc-{d,u,p}-law`), fed through a DCR, with 730 days of Analytics retention set on the table.
- **C5 Dashboard.** A new Azure Portal Dashboard, one per environment, separate from `monitoring-dashboard`.
  - Tiles: pie chart, daily availability % column chart, stacked daily counts with a `99.9` reference line, range totals, and a table on demand.
  - All tiles query C4 de-duplicated by `arg_max`. Range totals use summed counts (SELC-6.2).
- **C6 Failure alerting.** An Azure Monitor alert sends to an action group that forwards to Slack: `prod_self_care_status` (PROD) and `selfcare_status_uat` (DEV, UAT).
  - Assumption: detection combines a failed job execution with a scheduled-query check for a missing D-1 row in C4. The second check also catches runs that never started.
- **C7 Identity and access.** The job's Managed Identity holds only the roles listed under Security expectations. Viewers get dashboard read access plus read access on C4.

### Flow

1. Trigger: scheduled nightly, or manual for backfill and on-demand runs.
2. C2 queries C1 for the reference date.
3. C2 builds the record.
4. C2 upserts it into C3.
5. C2 ingests it into C4.
6. On failure, the job exits with an error and C6 notifies Slack. A later run realigns C3 and C4 (SELC-4.9).
7. C5 reads C4.

### Assumptions

- **A1 Schedule.** Container Apps cron runs in UTC, so 03:00 Europe/Rome means 01:00 UTC (summer) or 02:00 UTC (winter).
  - Provisional approach: trigger at both `0 1,2 * * *` UTC and rely on idempotency (SELC-1.3).
  - Alternative: the job skips the run unless the local time is 03:xx.
  - TO BE DECIDED.
- **A2 Backfill.** Run C2 manually, once per past date, with a date override. This is the same code path as the daily run.
- **A3 Environment isolation.** No cross-environment component. Each `-ar` stack owns its job, table, LA table, dashboard and alert.

### Gaps found in current infrastructure (need input)

- **G1 Source workspace not in IaC.** In `_modules/appgateway/app_gateway/main.tf` the App Gateway diagnostic setting only exists when `sec_log_analytics_workspace_id` is set, and `_modules/appgateway/main.tf` does not pass it. Which workspace receives `ApplicationGatewayAccessLog`, and with what retention: UNKNOWN.
- **G2 Retention mismatch.** `law_retention_in_days = 30` in `infra/core/{dev,uat,prod}-ar/locals.tf`, while SELC-5.2 and SELC-5.4 assume 90 days. The backfill depth equals the actual retention of the source table: TO BE DECIDED or verified.
- **G3 No Table private endpoint.** The logs storage (`_modules/storage_account_template`) has public network access disabled and a private endpoint for `Blob` only. Table access needs a `table` private endpoint, plus `privatelink.table.core.windows.net` resolution from the job's subnet. That zone exists today only in `_modules/synthetic_monitoring_storage`.
- **G4 Time filter vs. reference date.** Logs Ingestion rejects a `TimeGenerated` more than 2 days in the past, so backfilled rows cannot carry the reference date in `TimeGenerated`. The dashboard time picker filters on `TimeGenerated`, so range selection (SELC-6.3) must filter on the reference-date column instead.
  - How the user picks the range on a Portal Dashboard, which has no parameters: TO BE DECIDED.
  - The on-demand table mechanism (SELC-6.7): TO BE DECIDED.
- **G5 Slack routing.** The existing action groups are `selcdev` (DEV), `selcuat` (UAT) and `selcperror` / `SlackPagoPA` (PROD), defined in `_modules/monitor/main.tf`. Their Slack addresses live in Key Vault.
  - Which action groups or addresses map to `prod_self_care_status` and `selfcare_status_uat`: UNKNOWN.
  - DEV → `selfcare_status_uat` differs from the existing DEV action group.

## Requirement Traceability

| Component / boundary | Requirements | Needs more architecture input |
|---|---|---|
| C1 Source logs | SELC-2.5, 2.6, 2.7, 5.2, 5.3 | G1 (workspace), G2 (retention / backfill depth) |
| C2 Availability job | SELC-1.1–1.4, 2.1–2.4, 3.1–3.3, 4.3, 4.5, 4.9, 5.4 | Runtime and framework TO BE DECIDED; A1 (UTC cron vs 03:00 Rome) |
| C3 `SelcAvailability` | SELC-4.1–4.7 | G3 (Table private endpoint / DNS) |
| C4 `SelcAvailability_CL` + DCR | SELC-4.8, 4.10, 4.11 | Column schema TO BE DECIDED; G4 |
| C5 Portal Dashboard | SELC-6.1–6.9 | G4 (range selection on reference date, SELC-6.3; on-demand table, SELC-6.7) |
| C6 Failure alerting | SELC-5.1, 4.9 | G5 (channel ↔ action group mapping); detection signal (assumption in C6) |
| C7 Identity and access | SELC-4.7, 6.8 | Role scopes (table vs account, workspace vs table) TO BE DECIDED |
| Backfill / on-demand (manual run of C2) | SELC-5.2, 5.3, 5.4 | G2, G4 |

## Dependency Rules

- Do not add a dependency when the standard library or a few lines of first-party code
will do.
- Prefer zero new dependencies. If a library is required, justify it in the PR
description.
- Only use libraries that are actively maintained (commit or release within the last 12
months).
- Only use the latest stable major version. No deprecated, abandoned, or pre-release
packages.
- Reject any library with known unpatched CVEs. Check before adding and on every update.
- Audit transitive dependencies, not just direct ones. A small direct dep with a large or
unvetted tree is a rejection.
- Pin exact versions with a committed lockfile. No floating ranges in production.
- Prefer libraries with a narrow scope, minimal dependencies of their own, and a clear
security track record.
