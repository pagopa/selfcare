mock_provider "azurerm" {
  mock_data "azurerm_monitor_action_group" {
    defaults = {
      id = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/monitor-rg/providers/Microsoft.Insights/actionGroups/alerts"
    }
  }
}

override_module {
  target = module.scheduled_job
  outputs = {
    container_app_job_id                  = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/jobs-rg/providers/Microsoft.App/jobs/availability"
    system_assigned_identity_principal_id = "00000000-0000-0000-0000-000000000002"
  }
}

variables {
  env_short                           = "d"
  environment                         = "DEV"
  location                            = "westeurope"
  monitor_resource_group_name         = "monitor-rg"
  container_app_resource_group_name   = "jobs-rg"
  container_app_environment_name      = "container-apps"
  container_app                       = { cpu = 0.5, memory = "1Gi" }
  key_vault_resource_group_name       = "security-rg"
  key_vault_name                      = "availability-test-kv"
  log_analytics_workspace_guid        = "00000000-0000-0000-0000-000000000003"
  log_analytics_workspace_resource_id = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/monitor-rg/providers/Microsoft.OperationalInsights/workspaces/logs"
  application_gateway_resource_id     = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/network-rg/providers/Microsoft.Network/applicationGateways/gateway"
  storage_account_id                  = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/storage-rg/providers/Microsoft.Storage/storageAccounts/availability"
  storage_account_name                = "availability"
  tags                                = { Environment = "Test" }
}

run "failed_executions_alert" {
  command = plan

  assert {
    condition     = azurerm_monitor_metric_alert.availability_job_failure.scopes == toset([module.scheduled_job.container_app_job_id])
    error_message = "The failure alert must monitor the availability job, not the environment or reporting table."
  }

  assert {
    condition = (
      one(azurerm_monitor_metric_alert.availability_job_failure.criteria).metric_namespace == "Microsoft.App/jobs" &&
      one(azurerm_monitor_metric_alert.availability_job_failure.criteria).metric_name == "Executions" &&
      one(azurerm_monitor_metric_alert.availability_job_failure.criteria).aggregation == "Total" &&
      one(azurerm_monitor_metric_alert.availability_job_failure.criteria).operator == "GreaterThan" &&
      one(azurerm_monitor_metric_alert.availability_job_failure.criteria).threshold == 0
    )
    error_message = "Any failed job execution must cross the alert threshold."
  }

  assert {
    condition = toset(one([
      for dimension in one(azurerm_monitor_metric_alert.availability_job_failure.criteria).dimension :
      dimension.values if dimension.name == "state" && dimension.operator == "Include"
    ])) == toset(["Failed"])
    error_message = "Successful, running and skipped executions must not trigger the failure alert."
  }

  assert {
    condition = toset(one([
      for dimension in one(azurerm_monitor_metric_alert.availability_job_failure.criteria).dimension :
      dimension.values if dimension.name == "executionName" && dimension.operator == "Include"
    ])) == toset(["*"])
    error_message = "Separate failed executions must generate independent alert instances."
  }

  assert {
    condition = (
      azurerm_monitor_metric_alert.availability_job_failure.enabled &&
      azurerm_monitor_metric_alert.availability_job_failure.auto_mitigate &&
      azurerm_monitor_metric_alert.availability_job_failure.frequency == "PT5M" &&
      azurerm_monitor_metric_alert.availability_job_failure.window_size == "PT15M" &&
      azurerm_monitor_metric_alert.availability_job_failure.severity == 2
    )
    error_message = "The failure alert must evaluate every five minutes over fifteen minutes and automatically resolve."
  }

  assert {
    condition = (
      data.azurerm_monitor_action_group.availability_failure.name == "selcdev" &&
      one(azurerm_monitor_metric_alert.availability_job_failure.action).action_group_id == data.azurerm_monitor_action_group.availability_failure.id &&
      azurerm_monitor_metric_alert.availability_job_failure.tags == var.tags
    )
    error_message = "DEV alerts must reuse the existing environment action group and tags."
  }
}

run "uat_notification_routing" {
  command = plan

  variables {
    env_short   = "u"
    environment = "UAT"
  }

  assert {
    condition     = data.azurerm_monitor_action_group.availability_failure.name == "selcuat"
    error_message = "UAT failures must notify the UAT status action group."
  }
}

run "prod_notification_routing" {
  command = plan

  variables {
    env_short   = "p"
    environment = "PROD"
  }

  assert {
    condition     = data.azurerm_monitor_action_group.availability_failure.name == "selcperror"
    error_message = "PROD failures must notify the production error action group."
  }
}
