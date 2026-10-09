locals {
  availability_environment = var.env_short == "d" ? "DEV" : var.env_short == "u" ? "UAT" : "PROD"

  availability_failure_action_group_id = var.env_short == "p" ? azurerm_monitor_action_group.error_action_group[0].id : (
    var.env_short == "u" ? azurerm_monitor_action_group.selfcare_status_uat[0].id :
    azurerm_monitor_action_group.selfcare_status_dev[0].id
  )

  availability_records_query = <<-KQL
    let rangeStart = startofday(datetime({TimeRange:start}));
    let rangeEnd = startofday(datetime({TimeRange:end})) + 1d;
    SelcAvailability_CL
    | where Environment == "${local.availability_environment}"
    | where ReferenceDate >= rangeStart and ReferenceDate < rangeEnd
    | summarize arg_max(GenerationTimestamp, CountLt500, CountGte500, Total, Availability) by ReferenceDate, Environment
  KQL

  availability_totals_query = <<-KQL
    let rangeStart = startofday(datetime({TimeRange:start}));
    let rangeEnd = startofday(datetime({TimeRange:end})) + 1d;
    let records = SelcAvailability_CL
      | where Environment == "${local.availability_environment}"
      | where ReferenceDate >= rangeStart and ReferenceDate < rangeEnd
      | summarize arg_max(GenerationTimestamp, CountLt500, CountGte500, Total, Availability) by ReferenceDate, Environment;
    records
    | summarize CountLt500 = sum(CountLt500), CountGte500 = sum(CountGte500)
    | extend Total = CountLt500 + CountGte500
    | extend Availability = iff(Total == 0, 100.0, round(100.0 * CountLt500 / Total, 2))
    | project CountLt500, CountGte500, Total, Availability
  KQL

  availability_pie_query = <<-KQL
    let rangeStart = startofday(datetime({TimeRange:start}));
    let rangeEnd = startofday(datetime({TimeRange:end})) + 1d;
    let records = SelcAvailability_CL
      | where Environment == "${local.availability_environment}"
      | where ReferenceDate >= rangeStart and ReferenceDate < rangeEnd
      | summarize arg_max(GenerationTimestamp, CountLt500, CountGte500, Total, Availability) by ReferenceDate, Environment;
    let totals = records | summarize CountLt500 = sum(CountLt500), CountGte500 = sum(CountGte500);
    print Status = "Available (<500)", Requests = toscalar(totals | project CountLt500)
    | union (print Status = "Unavailable (>=500 or invalid)", Requests = toscalar(totals | project CountGte500))
  KQL

  availability_workbook = {
    version            = "Notebook/1.0"
    defaultResourceIds = [var.log_analytics_workspace_id]
    items = [
      {
        type = 9
        content = {
          version   = "KqlParameterItem/2.0"
          style     = "formHorizontal"
          queryType = 0
          parameters = [
            {
              id         = "time-range"
              name       = "TimeRange"
              label      = "Reference date range"
              type       = 4
              isRequired = true
              value = {
                durationMs            = 2592000000
                grain                 = "1d"
                useDashboardTimeRange = false
                includeCustom         = true
                availableDurationMs   = [86400000, 604800000, 2592000000, 7776000000, 63072000000]
              }
            }
          ]
        }
      },
      {
        type = 3
        name = "request-counts"
        content = {
          version       = "KqlItem/1.0"
          title         = "Available and unavailable requests"
          query         = local.availability_pie_query
          queryType     = 0
          resourceType  = "microsoft.operationalinsights/workspaces"
          resourceIds   = [var.log_analytics_workspace_id]
          size          = 1
          visualization = "piechart"
          chartSettings = {
            xAxis      = "Status"
            yAxis      = ["Requests"]
            showLegend = true
          }
        }
      },
      {
        type = 3
        name = "daily-availability"
        content = {
          version       = "KqlItem/1.0"
          title         = "Daily availability (99.9% target)"
          query         = "${local.availability_records_query}\n| project ReferenceDate, Availability\n| order by ReferenceDate asc"
          queryType     = 0
          resourceType  = "microsoft.operationalinsights/workspaces"
          resourceIds   = [var.log_analytics_workspace_id]
          size          = 1
          visualization = "barchart"
          chartSettings = {
            xAxis                    = "ReferenceDate"
            yAxis                    = ["Availability"]
            showLegend               = false
            customThresholdLine      = "99.9"
            customThresholdLineStyle = 2
            ySettings = {
              min = 0
              max = 100
            }
          }
        }
      },
      {
        type = 3
        name = "daily-request-counts"
        content = {
          version       = "KqlItem/1.0"
          title         = "Daily request counts"
          query         = "${local.availability_records_query}\n| project ReferenceDate, CountLt500, CountGte500\n| order by ReferenceDate asc\n| render barchart with (kind=stacked)"
          queryType     = 0
          resourceType  = "microsoft.operationalinsights/workspaces"
          resourceIds   = [var.log_analytics_workspace_id]
          size          = 1
          visualization = "barchart"
          chartSettings = {
            xAxis      = "ReferenceDate"
            yAxis      = ["CountLt500", "CountGte500"]
            showLegend = true
            seriesLabelSettings = [
              {
                seriesName = "CountLt500"
                label      = "Available (<500)"
                color      = "green"
              },
              {
                seriesName = "CountGte500"
                label      = "Unavailable (>=500 or invalid)"
                color      = "red"
              }
            ]
          }
        }
      },
      {
        type = 3
        name = "range-totals"
        content = {
          version       = "KqlItem/1.0"
          title         = "Totals for selected reference-date range"
          query         = local.availability_totals_query
          queryType     = 0
          resourceType  = "microsoft.operationalinsights/workspaces"
          resourceIds   = [var.log_analytics_workspace_id]
          size          = 1
          visualization = "table"
        }
      },
      {
        type = 12
        name = "daily-records-group"
        content = {
          version        = "NotebookGroup/1.0"
          groupType      = "editable"
          loadType       = "explicit"
          loadButtonText = "Show daily records"
          title          = "Daily records"
          items = [
            {
              type = 3
              name = "daily-records"
              content = {
                version       = "KqlItem/1.0"
                title         = "Daily availability records"
                query         = "${local.availability_records_query}\n| project ReferenceDate, Environment, CountLt500, CountGte500, Total, Availability\n| order by ReferenceDate desc"
                queryType     = 0
                resourceType  = "microsoft.operationalinsights/workspaces"
                resourceIds   = [var.log_analytics_workspace_id]
                size          = 1
                visualization = "table"
              }
            }
          ]
        }
      }
    ]
  }
}

resource "azurerm_log_analytics_workspace_table" "azure_diagnostics" {
  name                    = "AzureDiagnostics"
  workspace_id            = var.log_analytics_workspace_id
  plan                    = "Analytics"
  retention_in_days       = 90
  total_retention_in_days = 90
}

resource "azurerm_log_analytics_workspace_table_custom_log" "availability" {
  name                    = "SelcAvailability_CL"
  workspace_id            = var.log_analytics_workspace_id
  display_name            = "Selfcare daily API availability"
  description             = "Append-only reporting copy of daily availability records."
  plan                    = "Analytics"
  retention_in_days       = 730
  total_retention_in_days = 730

  column {
    name = "TimeGenerated"
    type = "dateTime"
  }
  column {
    name = "ReferenceDate"
    type = "dateTime"
  }
  column {
    name = "Environment"
    type = "string"
  }
  column {
    name = "CountLt500"
    type = "long"
  }
  column {
    name = "CountGte500"
    type = "long"
  }
  column {
    name = "Total"
    type = "long"
  }
  column {
    name = "Availability"
    type = "real"
  }
  column {
    name = "GenerationTimestamp"
    type = "dateTime"
  }
}

resource "azurerm_application_insights_workbook" "availability_daily" {
  name                = uuidv5("dns", "selc-${var.env_short}-availability-daily")
  resource_group_name = var.monitor_rg_name
  location            = var.monitor_rg_location
  display_name        = "Selfcare Daily API Availability (${local.availability_environment})"
  description         = "Daily Selfcare API availability by reference date."
  category            = "workbook"
  source_id           = lower(var.log_analytics_workspace_id)
  data_json           = jsonencode(local.availability_workbook)
  tags                = var.tags
}

resource "azurerm_monitor_scheduled_query_rules_alert_v2" "availability_missing_daily_record" {
  name                    = "${local.project}-availability-missing-daily-record"
  display_name            = "Daily API availability record missing (${local.availability_environment})"
  description             = "The previous UTC day's availability record was not ingested after the scheduled run window."
  resource_group_name     = var.monitor_rg_name
  location                = var.monitor_rg_location
  scopes                  = [var.log_analytics_workspace_id]
  severity                = 2
  enabled                 = true
  auto_mitigation_enabled = true
  evaluation_frequency    = "PT1H"
  window_duration         = "PT1H"
  # Search beyond the evaluation hour so a successful overnight run remains visible.
  query_time_range_override = "P2D"
  tags                      = var.tags

  criteria {
    query = <<-KQL
      let expectedDate = startofday(now(-1d));
      let runWindowPassed = now() >= startofday(now()) + 4h;
      SelcAvailability_CL
      | where TimeGenerated >= ago(2d)
      | where Environment == "${local.availability_environment}"
      | where ReferenceDate == expectedDate
      | summarize recordCount = count()
      | where recordCount == 0 and runWindowPassed
      | extend Environment = "${local.availability_environment}",
               ReferenceDate = format_datetime(expectedDate, "yyyy-MM-dd"),
               FailureReason = "No reporting record was ingested; inspect the scheduled job execution."
      | project MissingRecord = 1, Environment, ReferenceDate, FailureReason
    KQL

    time_aggregation_method = "Maximum"
    metric_measure_column   = "MissingRecord"
    operator                = "GreaterThan"
    threshold               = 0

    dimension {
      name     = "Environment"
      operator = "Include"
      values   = ["*"]
    }

    dimension {
      name     = "ReferenceDate"
      operator = "Include"
      values   = ["*"]
    }

    failing_periods {
      minimum_failing_periods_to_trigger_alert = 1
      number_of_evaluation_periods             = 1
    }
  }

  action {
    action_groups = [local.availability_failure_action_group_id]
  }

  depends_on = [azurerm_log_analytics_workspace_table_custom_log.availability]
}
