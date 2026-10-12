mock_provider "azurerm" {}

variables {
  env_short                  = "d"
  subscription_id            = "00000000-0000-0000-0000-000000000001"
  key_vault_id               = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/security-rg/providers/Microsoft.KeyVault/vaults/availability-test-kv"
  monitor_rg_name            = "monitor-rg"
  monitor_rg_location        = "westeurope"
  application_insights_id    = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/monitor-rg/providers/Microsoft.Insights/components/insights"
  application_insights_name  = "insights"
  log_analytics_workspace_id = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/monitor-rg/providers/Microsoft.OperationalInsights/workspaces/logs"
  dns_a_api_fqdn             = "api.example.test"
  dns_a_api_pnpg_fqdn        = "api-pnpg.example.test"
  cdn_fqdn                   = "cdn.example.test"
  selfcare_status_dev_email  = "alerts@example.test"
  selfcare_status_dev_slack  = "slack-alerts@example.test"
  tags                       = { Environment = "Test" }
}

run "missing_record_lookback" {
  command = plan

  assert {
    condition = (
      azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.evaluation_frequency == "PT1H" &&
      azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.window_duration == "PT1H" &&
      azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.query_time_range_override == "P2D"
    )
    error_message = "An hourly evaluation must still see overnight records through a separate 48-hour query lookback."
  }

  assert {
    condition = (
      strcontains(one(azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.criteria).query, "startofday(now(-1d))") &&
      strcontains(one(azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.criteria).query, "startofday(now()) + 4h") &&
      strcontains(one(azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.criteria).query, "TimeGenerated >= ago(2d)") &&
      strcontains(one(azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.criteria).query, "recordCount == 0 and runWindowPassed")
    )
    error_message = "Only a missing D-1 UTC record after the overnight grace period must trigger the alert."
  }

  assert {
    condition = toset([
      for dimension in one(azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.criteria).dimension :
      dimension.name
    ]) == toset(["Environment", "ReferenceDate"])
    error_message = "Notifications must identify the environment and reference date."
  }

  assert {
    condition = (
      azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.tags == var.tags &&
      one(azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.criteria).threshold == 0 &&
      azurerm_monitor_scheduled_query_rules_alert_v2.availability_missing_daily_record.auto_mitigation_enabled
    )
    error_message = "Missing records must trigger an automatically resolving, tagged alert."
  }
}
