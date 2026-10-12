# Daily API availability

The scheduled job computes the previous UTC day at 03:00 Europe/Rome. Its cron schedule is evaluated in UTC, so it runs at 01:00 and 02:00 UTC; the application proceeds only when it starts during the 03:00–03:59 hour in Italy and skips the other trigger.

## Deployment order

Apply the matching core stack (`infra/core/<env>-ar`) before the resource stack here. The core stack enables Application Gateway diagnostics, configures source-log retention, creates `SelcAvailability_CL` and its Workbook/alert, and provisions the Storage Table private endpoint and DNS. Then apply the resource stack to create the ingestion rule and scheduled job. Historical backfill can include only complete days already present in the workspace; diagnostics collected after deployment are not retroactive.

After both stacks are applied, run `--backfill` once per environment to populate retained history. Confirm the environment's Slack action-group receiver is configured before relying on failure notifications.

To run a single retained date or backfill complete retained dates, start the job with an execution-template override. First save the current template:

```sh
az containerapp job show \
  --name "selc-<env-short>-availability-daily-job" \
  --resource-group "selc-<env-short>-container-app-002-rg" \
  --query "properties.template" \
  --output yaml > availability-job-template.yaml
```

Set the first container's `args` to either a single ISO date (`["2026-01-31"]`) or `["--backfill"]`, then start the execution:

```sh
az containerapp job start \
  --name "selc-<env-short>-availability-daily-job" \
  --resource-group "selc-<env-short>-container-app-002-rg" \
  --yaml availability-job-template.yaml
```

The date must be within the complete-day source retention window. The backfill skips the partially expired boundary day. Starting a job allows execution with its Managed Identity; limit job-start permissions to trusted operators.
