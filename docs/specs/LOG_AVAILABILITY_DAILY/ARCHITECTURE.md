# LOG_AVAILABILITY_DAILY — Architecture

Primary source of truth: [REQUIREMENTS.md](./REQUIREMENTS.md). Requirement IDs (`SELC-x.y`) refer to that file. Infrastructure facts come from `infra/core/*` and `infra/resources/*`.

## Required Architecture Inputs

- Requirements source: REQUIREMENTS.md
- System purpose: Each night, compute the previous UTC day's API availability for each environment (DEV, UAT, PROD) from Application Gateway access logs (`< 500` available vs `>= 500`/invalid unavailable). Store one record per day and report it on an Azure Portal Dashboard over any date range.
- Primary use cases: (1) scheduled D-1 computation and persistence (SELC-1, 2, 3, 4); (2) go-live backfill and on-demand regeneration of a past date (SELC-5.2–5.4); (3) failure notification (SELC-5.1); (4) range reporting: pie chart, daily column charts, totals, on-demand table (SELC-6).
- Target users / actors: Scheduled job (system actor, Managed Identity). Operators receiving Slack failure notifications. Report consumers: users of the environment's Azure subscription (SELC-6.8).
- Runtime environment: Azure Container Apps Job in each environment's existing Container Apps Environment; Terraform module `infra/resources/_modules/container_app_job`; Docker image in `ghcr.io/pagopa/*`. Scheduled Container Apps cron expressions are evaluated in UTC.
- Server framework: Quarkus 3.31.x / Java 17 command-mode application (`@QuarkusMain`), matching repo conventions. No inbound HTTP server is required.
- Client framework: Azure Monitor Workbook (`azurerm_application_insights_workbook`) with Log Analytics (KQL) query items and a time-range parameter that filters by `ReferenceDate` (SELC-6.3).
- API style and integration model: No inbound API. Outbound-only batch over HTTPS with Entra ID tokens:
  - Log Analytics query (KQL over `AzureDiagnostics`, App Gateway access logs).
  - Azure Table Storage upsert into `SelcAvailability`.
  - Azure Monitor Logs Ingestion API through a Data Collection Rule into `SelcAvailability_CL`. This is the only Managed-Identity path into a custom table; the legacy HTTP Data Collector API needs a shared key, which SELC-4.7 forbids.
  - Failure notification through Azure Monitor alert → action group → email-to-Slack receiver (existing pattern in `infra/core/_modules/monitor`).
- Authentication and session model: No user sessions and no application-level login.
  - Writer: system-assigned Entra ID Managed Identity on the scheduled Container Apps Job. Keys, SAS and connection strings MUST NOT be used for writes (SELC-4.7).
  - Readers: Entra ID users of the subscription through the Azure Portal. Dashboard tiles query with the viewer's identity (SELC-6.8).
- Data model expectations: One Daily Availability Record per (reference date, environment): reference date, environment, `count_lt_500`, `count_gte_500`, total, availability (2 decimal places), generation timestamp (SELC-4.2, 3.3).
  - `SelcAvailability` (system of record, no expiry): `PartitionKey = yyyy`, `RowKey = yyyy-MM-dd` (UTC); upsert replaces the record.
  - `SelcAvailability_CL` (reporting copy, append-only, 730-day Analytics retention): `TimeGenerated`, `ReferenceDate`, and `GenerationTimestamp` are datetime columns; `Environment` is string; counts are long; `Availability` is real. The latest entry per key is selected with `arg_max(GenerationTimestamp)` (SELC-4.10).
- Deployment model: Terraform, one stack per environment: `infra/core/{dev,uat,prod}-ar`, plus `infra/resources/log-availability-runner/{dev,uat,prod}-ar` for the job. PNPG stacks (`*-pnpg`) are not involved: PNPG traffic is in the `-ar` gateway (`api-pnpg` listener).
  - New resources: the `SelcAvailability` table in `selc{d,u,p}stlogs`; a Table private endpoint; the `SelcAvailability_CL` table and its DCR; the dashboard; the failure alert; role assignments.
  - Core stack owns App Gateway diagnostics, retention settings, the storage-table private endpoint, the custom Log Analytics table, Workbook, and missing-record alert. The app resource stack owns the job, DCR/DCE, table creation permissions, Managed Identity role assignments, and failed-execution alert.
- Scale expectations: Very low write volume: 1 scheduled execution per day per environment (plus idempotent reruns) and 1 record per day per environment (~365 per year).
  - Read side: one 24-hour KQL aggregation over App Gateway access logs. Daily log volume: UNKNOWN. The gateway is WAF_v2 with autoscale 1–5 in PROD.
  - Backfill: one-off, bounded by the source log retention.
  - No latency target beyond completing the nightly run.
- Security expectations:
  - The job uses its system-assigned Managed Identity. Least-privilege RBAC: `Log Analytics Reader` on the environment workspace; `Storage Table Data Contributor` on the logs storage account; `Monitoring Metrics Publisher` on the DCR.
  - Table storage traffic uses a Table private endpoint. The logs account retains its existing public-network setting: enabled in DEV/UAT and disabled in PROD.
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
- **C4 Reporting copy.** Custom table `SelcAvailability_CL` in the environment workspace (`selc-{d,u,p}-law`), fed through a DCR, with 730 days of Analytics retention set on the table. `TimeGenerated` is ingestion/generation time; `ReferenceDate` carries the day being reported.
- **C5 Workbook.** A new Azure Monitor Workbook, one per environment, separate from `monitoring-dashboard`.
  - Tiles: pie chart, daily availability % column chart, stacked daily counts with a `99.9` reference line, range totals, and a table on demand.
  - Workbook time-range parameter filters `ReferenceDate`; all queries select the latest C4 record per key with `arg_max`. Range totals use summed counts (SELC-6.2).
- **C6 Failure alerting.** Two Azure Monitor alerts send to the existing environment action group, whose Key Vault-backed receiver routes to Slack: `prod_self_care_status` (PROD) and `selfcare_status_uat` (DEV, UAT). The DEV receiver secret must target the UAT channel; this out-of-band configuration is a deployment prerequisite.
  - Failed execution: `Microsoft.App/jobs` metric `Executions`, `state = Failed`, threshold `> 0`, evaluated every 5 minutes over 15 minutes. Split by `executionName` so subsequent failed executions are detected independently, including manual recalculations with an existing daily record. Azure retries occur before the execution reaches its terminal failed state. The alert identifies the job/execution; its logs contain the reference date and failure stage (`SOURCE_QUERY`, `TABLE_STORAGE_WRITE`, `LOG_ANALYTICS_WRITE`).
  - Missing record: hourly scheduled query after 04:00 UTC, checking for D-1 in C4 with an explicit 48-hour ingestion-time lookback, independent of the selected reporting dates. Environment and UTC reference date are alert dimensions. This detects runs that never started or did not deliver a reporting record; the alert resolves once the record is visible.
- **C7 Identity and access.** The job's Managed Identity holds only the roles listed under Security expectations. Viewers get dashboard read access plus read access on C4.

### Flow

1. Trigger: scheduled nightly, or manual for backfill and on-demand runs.
2. C2 queries C1 for the reference date.
3. C2 builds the record.
4. C2 upserts it into C3.
5. C2 ingests it into C4.
6. On failure, the job exits with an error and C6 notifies Slack. A later run realigns C3 and C4 (SELC-4.9).
7. C5 reads C4 using the selected reference-date range.

### Assumptions

- **A1 Schedule.** Container Apps cron runs in UTC, so trigger at both `0 1,2 * * *` UTC. The job proceeds without an explicit reference date when it starts during the 03:00–03:59 hour in `Europe/Rome`; it skips the other trigger. Accepting the full hour tolerates job startup delay while daylight-saving changes still select one trigger per day (SELC-1.3). Manual date-specific runs bypass this guard.
- **A2 Backfill.** Run C2 manually, once per past date, with a date override. This is the same code path as the daily run.
- **A3 Environment isolation.** No cross-environment component. Each `-ar` stack owns its job, table, LA table, dashboard and alert.

### Infrastructure changes and remaining limits

- **G1 Source workspace.** Terraform routes App Gateway access diagnostics to each existing environment workspace (`selc-{d,u,p}-law`). Collection begins after deployment; prior history can be backfilled only if it already exists in that workspace.
- **G2 Retention.** Terraform configures 90-day retention for the `AzureDiagnostics` table to support SELC-5.2/5.4 without changing retention for unrelated tables. The oldest partial day is skipped.
- **G3 Table network path.** Terraform adds a Table private endpoint and links `privatelink.table.core.windows.net` to the core VNet used by the Container Apps environment. DEV/UAT retain their existing public-network-enabled setting; PROD remains disabled.
- **G4 Reporting time range.** Logs Ingestion limits historical `TimeGenerated`, so the reporting copy uses ingestion time there and stores `ReferenceDate` separately. The Workbook time-range parameter filters on `ReferenceDate`; its table item implements the on-demand daily records view.
- **G5 Slack routing.** Reuse the existing action groups and Key Vault-backed receivers. DEV's `alert-selfcare-status-dev-slack` secret is updated out of band to point to `selfcare_status_uat`; UAT uses its existing UAT receiver. PROD receiver configuration remains in the existing production error action group.
- **G6 Eventual consistency.** Azure Table Storage and Log Analytics cannot participate in a shared transaction. A destination failure may leave a partial write; the run fails and a retry repairs the projection. `SelcAvailability` remains authoritative (SELC-4.5, 4.9).

## Requirement Traceability

| Component / boundary | Requirements | Needs more architecture input |
|---|---|---|
| C1 Source logs | SELC-2.5, 2.6, 2.7, 5.2, 5.3 | G1 (collection starts after deployment); G2 (available history / backfill depth) |
| C2 Availability job | SELC-1.1–1.4, 2.1–2.4, 3.1–3.3, 4.3, 4.5, 4.9, 5.4 | G1 (pre-existing history), G6 (temporary partial writes) |
| C3 `SelcAvailability` | SELC-4.1–4.7 | G3 (private path must be verified after deployment) |
| C4 `SelcAvailability_CL` + DCR | SELC-4.8, 4.10, 4.11 | Column schema TO BE DECIDED; G4 |
| C5 Azure Monitor Workbook | SELC-6.1–6.9 | Workbook KQL/rendering and date-range parameter |
| C6 Failure alerting | SELC-5.1, 4.9 | G5 (receiver secret values must be correct); verify metric emission and notification delivery after deployment |
| C7 Identity and access | SELC-4.7, 6.8 | Viewer role assignments are managed outside this feature |
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
