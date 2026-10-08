# LOG_AVAILABILITY_DAILY — Requirements

Daily API availability log for the Selfcare platform (PagoPA), computed from the previous day's Application Gateway access logs and stored for date-based reporting.

Reference queries: `azurerm_portal_dashboard.monitoring-dashboard` in `infra/core/_modules/monitor/main.tf` (template `infra/core/_modules/dashboards/monitoring.json.tpl`). Application Gateway: `module "app_gw"` in `infra/core/_modules/appgateway/main.tf`. There is one gateway for each `-ar` environment, with listener `api` for SELC and listener `api-pnpg` for PNPG.

## Required Requirement Inputs

- Project purpose: Selfcare platform (PagoPA) for managing identity, onboarding, products and webhooks for the Italian public sector, on two platforms: SELC (Self Care) and PNPG (Piattaforma Notifiche PG). This feature produces one availability record per day for APIs served through Application Gateway, so reports can cover any date range.
- Primary users / actors: Scheduled daily job (system actor); report consumers: users of the environment's Azure subscription.
- Core workflows: (1) daily computation of D-1 request counts split by HTTP status `< 500` / `>= 500`; (2) availability calculation; (3) persistence to a Storage Account table; (4) reporting dashboard over a selected time range: pie chart, daily column chart, numeric and percentage totals, on-demand data table.
- Business objects / data entities: Application Gateway access log entries (`AzureDiagnostics`, Category `ApplicationGatewayAccessLog`); Daily Availability Record (date, counts, availability).
- External integrations: Azure Log Analytics workspace (Application Gateway diagnostics, and a custom table holding the reporting copy of the records), Azure Storage Account Table (`SelcAvailability` in `selc{d,u,p}stlogs`). Dashboard: new Azure Portal Dashboard. Failure notifications: Slack (`prod_self_care_status`, `selfcare_status_uat`).
- Authentication / roles: The writer (the job that generates records) authenticates with Azure Managed Identity. Readers are users of the Azure subscription who can access the dashboard.
- Regulatory or privacy constraints: none (confirmed decision). Only aggregated counts are stored; no personal data from source logs (e.g. client IP, request URI) is persisted (SELC-4.4).

## Functional Requirements

### 1. Daily scheduling

- **SELC-1.1** The system MUST generate the availability log for the previous calendar day (D-1) automatically, at least once per day, without manual action.
- **SELC-1.2** A day MUST cover `[D-1 00:00:00Z, D 00:00:00Z)` in UTC (confirmed decision).
- **SELC-1.3** The system MAY run several times per day for the same D-1, for example to pick up late-ingested logs. All runs for the same D-1 and environment MUST be idempotent: recomputing from the same source data MUST give the same record, and the latest successful run MUST replace earlier results (SELC-4.3). Confirmed decision. The scheduled run MUST take place at night at 03:00 Italian time (Europe/Rome, confirmed decision). That is 01:00 or 02:00 UTC depending on daylight saving time: after D-1 has ended in UTC, with time left for late log ingestion.
- **SELC-1.4** The run MUST produce one result per environment: DEV, UAT and PROD (confirmed decision). Each environment's result covers the Application Gateway deployed in that environment's `-ar` stack, which serves both listeners.

### 2. Request counting

- **SELC-2.1** For D-1, the system MUST count the API requests with a valid HTTP status code `< 500`, i.e. `100–499` (`count_lt_500`). These are the available requests, including all `4xx`. Confirmed decision: this deliberately differs from the stricter success filters in the monitoring dashboard (`< 300 or == 404`, status allow-lists).
- **SELC-2.2** For D-1, the system MUST count the API requests whose HTTP status code is `>= 500` (`count_gte_500`). This count also includes the requests covered by SELC-2.4.
- **SELC-2.3** Counts MUST be cumulative for the whole day (a single daily total, see SELC-2.7; not hourly or 5-minute bins).
- **SELC-2.4** Requests with a missing HTTP status or an invalid one (empty, non-numeric, `0`, or outside `100–599`) MUST be counted as unavailable, i.e. added to `count_gte_500` (confirmed decision). Each request MUST be counted in exactly one bucket.
- **SELC-2.5** Counts MUST be computed only from Application Gateway access logs, using `httpStatus_d` as the status code. Application Insights data MUST NOT contribute to the counts, so that requests are not double-counted and sampling does not distort them. Confirmed decision.
- **SELC-2.6** Requests with `requestUri_s == "/spid/v1/metadata"` or `requestUri_s == "dummy"` MUST be excluded from all counts (confirmed decision). The exclusions MUST be the same every day.
- **SELC-2.7** The counts MUST be a single combined daily total across the `api` (SELC) and `api-pnpg` (PNPG) listeners. They MUST NOT be split by platform or API path. Requests on other listeners MUST NOT be counted. Confirmed decision: all SELC and PNPG API traffic passes through these two listeners.

### 3. Availability calculation

- **SELC-3.1** The system MUST compute `availability = count_lt_500 / (count_lt_500 + count_gte_500) * 100` for each record.
- **SELC-3.2** When `count_lt_500 + count_gte_500 = 0`, the system MUST store `availability = 100` and MUST NOT divide by zero (confirmed decision).
- **SELC-3.3** Availability MUST be expressed as a percentage rounded to 2 decimal places, e.g. `99.95` (confirmed decision). The same precision MUST apply to the totals over a range (SELC-6.2). The raw counts MUST be stored alongside it so availability can be recomputed.

### 4. Persistence

- **SELC-4.1** The system MUST store each daily result in the new Azure Table `SelcAvailability` of the environment's logs Storage Account: DEV → `selcdstlogs`, UAT → `selcustlogs`, PROD → `selcpstlogs` (confirmed decision).
- **SELC-4.2** Each record MUST include: reference date (D-1), environment, `count_lt_500`, `count_gte_500`, total, availability and generation timestamp. In `SelcAvailability`, `PartitionKey` MUST be the year of the reference date (`yyyy`) and `RowKey` MUST be the reference date (`yyyy-MM-dd`, UTC) (confirmed decision).
- **SELC-4.3** Writes MUST be idempotent: one record per (reference date, environment), which is one `PartitionKey`/`RowKey` pair in each environment's table. Every run, whether scheduled, repeated or on-demand, MUST replace the existing record for that key and MUST NOT create a duplicate.
- **SELC-4.4** Records MUST NOT contain personal data or raw request details (client IP, full URI, query string, user identifiers).
- **SELC-4.5** A failed run (e.g. a log query error or timeout) MUST NOT write or overwrite a record for the affected date in either destination (SELC-4.1, SELC-4.8). A zero-count record (SELC-3.2) MUST only come from a successful query that returned no requests.
- **SELC-4.6** `SelcAvailability` records MUST NOT expire. The system MUST NOT delete them automatically (confirmed decision).
- **SELC-4.7** Writes to `SelcAvailability` and to the Log Analytics reporting table (SELC-4.8) MUST authenticate with a Managed Identity. Storage account keys, SAS tokens and connection strings MUST NOT be used for writing (confirmed decision).
- **SELC-4.8** For every record written to `SelcAvailability`, the system MUST also write the same record to a custom Log Analytics table that the dashboard reads (confirmed decision). `SelcAvailability` remains the system of record. The custom table is named `SelcAvailability_CL` (confirmed decision).
- **SELC-4.9** If either write fails, the run MUST be treated as failed (SELC-5.1). A later run MUST bring both destinations back in line (SELC-1.3).
- **SELC-4.10** The Log Analytics reporting table is append-only, so it can hold several entries for the same (reference date, environment). Reporting MUST use only the entry with the latest generation timestamp for each such key.
- **SELC-4.11** The Log Analytics reporting table MUST keep 730 days (2 years, the maximum) of Analytics, i.e. queryable, retention. It MUST NOT inherit the workspace default of 90 days (confirmed decision). The dashboard can therefore report on at most the last 730 days. Older history is available only in `SelcAvailability`.

### 5. Failure handling and backfill

- **SELC-5.1** A failed daily run MUST be detectable by operators. A notification MUST be sent to Slack: channel `prod_self_care_status` for PROD, and `selfcare_status_uat` for DEV and UAT (confirmed decision). The notification SHOULD include the environment, the reference date and the failure reason.
- **SELC-5.2** At go-live, the system MUST backfill one record per environment for every past day still available in Log Analytics. Retention is 90 days, so roughly the last 90 days (confirmed decision). Backfill MUST follow the same rules as the daily run (SELC-2.x, SELC-3.x) and MUST be idempotent (SELC-4.3).
- **SELC-5.3** The system MUST NOT generate a record for a day that is only partly within retention. The oldest boundary day has been partly purged and would give understated counts.
- **SELC-5.4** The system SHOULD support on-demand generation for any single past date within the 90-day retention, applying SELC-4.3.

### 6. Reporting

- **SELC-6.1** Stored records MUST be retrievable for any date or date range by reference date, with optional filtering by environment.
- **SELC-6.2** Totals over a date range MUST be computed from summed daily counts, not by averaging daily percentages: `availability = Σcount_lt_500 / (Σcount_lt_500 + Σcount_gte_500) * 100`, and 100 when the sum is 0.
- **SELC-6.3** Reporting MUST be provided by a new Azure Portal Dashboard, separate from the existing `monitoring-dashboard` (confirmed decision). The dashboard tiles MUST read data from the Log Analytics reporting table (SELC-4.8, SELC-4.10). The user MUST be able to choose a time range (start and end date), and the dashboard MUST show one environment at a time. Everything in SELC-6.4–6.7 MUST reflect the selected range.
- **SELC-6.4** The dashboard MUST show a pie chart of `count_lt_500` vs `count_gte_500` over the selected range.
- **SELC-6.5** The dashboard MUST show daily column charts with one column per day in the selected range, showing both daily availability % and the stacked `count_lt_500` / `count_gte_500` (confirmed decision). They MAY be two separate charts.
- **SELC-6.6** The dashboard MUST show, for the selected range, the totals as numbers (`count_lt_500`, `count_gte_500`, total) and the availability as a percentage (SELC-6.2).
- **SELC-6.7** On user request, the dashboard MUST show a table of the daily records in the selected range: reference date, environment, `count_lt_500`, `count_gte_500`, total, availability.
- **SELC-6.8** The dashboard consumers are users of the environment's Azure subscription. Read access MUST be granted through the shared Azure Portal Dashboard, with no separate reporting credential (confirmed decision). Dashboard tiles run their queries with the viewer's identity, so these users MUST also have read access to the Log Analytics reporting table. Otherwise the tiles show no data.
- **SELC-6.9** The availability target is `99.9%` (confirmed decision). The dashboard SHOULD show this target as a reference line, as the existing dashboards do with their `watermark`.

## Open Questions

None. All open questions have been resolved.
