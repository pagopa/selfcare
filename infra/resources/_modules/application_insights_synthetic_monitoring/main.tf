locals {
  check_name   = "webhook-diagnostics"
  run_location = "private"
}

data "azurerm_application_insights" "this" {
  name                = var.application_insight_name
  resource_group_name = var.application_insight_rg_name
}

data "azurerm_storage_account" "this" {
  name                = var.storage_account_name
  resource_group_name = var.storage_account_resource_group_name
}

resource "azurerm_storage_table" "configuration" {
  name               = "monitoringconfiguration"
  storage_account_id = data.azurerm_storage_account.this.id
}

resource "azurerm_storage_table_entity" "diagnostics" {
  storage_table_id = azurerm_storage_table.configuration.id
  partition_key    = local.check_name
  row_key          = local.run_location

  entity = {
    url           = var.diagnostics_url
    method        = "GET"
    expectedCodes = jsonencode(["200"])
    durationLimit = "5000"
  }
}

resource "azurerm_container_app_job" "this" {
  name                         = "${var.prefix}-synthmon-caj"
  resource_group_name          = var.resource_group_name
  location                     = var.location
  container_app_environment_id = var.container_app_environment_id

  identity {
    type         = "UserAssigned"
    identity_ids = [var.user_assigned_identity_id]
  }

  schedule_trigger_config {
    cron_expression          = var.cron_scheduling
    parallelism              = 1
    replica_completion_count = 1
  }

  workload_profile_name = "Consumption"

  template {
    container {
      name    = "synthetic-monitoring"
      image   = "ghcr.io/pagopa/selfcare-webhook-ms:${local.sanitized_image_tag}"
      cpu     = 0.25
      memory  = "0.5Gi"
      command = ["java"]
      args = [
        "-javaagent:/app/applicationinsights-agent.jar",
        "-cp",
        "/app/app/*:/app/lib/main/*:/app/lib/boot/*",
        "it.pagopa.selfcare.webhook.monitoring.SyntheticMonitoringMain"
      ]

      env {
        name  = "APPLICATIONINSIGHTS_CONNECTION_STRING"
        value = data.azurerm_application_insights.this.connection_string
      }
      env {
        name  = "APPLICATIONINSIGHTS_ROLE_NAME"
        value = "webhook-synthetic-monitoring"
      }
      env {
        name  = "OTEL_METRIC_EXPORT_INTERVAL"
        value = "5000"
      }
      env {
        name  = "METRIC_EXPORT_WAIT_MS"
        value = "6000"
      }
      env {
        name  = "STORAGE_ACCOUNT_NAME"
        value = data.azurerm_storage_account.this.name
      }
      env {
        name  = "STORAGE_ACCOUNT_TABLE_NAME"
        value = azurerm_storage_table.configuration.name
      }
      env {
        name  = "AZURE_LOCATION"
        value = var.location
      }
      env {
        name  = "AZURE_CLIENT_ID"
        value = var.user_assigned_identity_client_id
      }
    }
  }

  replica_retry_limit        = 1
  replica_timeout_in_seconds = 300

  tags = var.tags

  depends_on = [
    azurerm_role_assignment.table_reader,
    azurerm_storage_table_entity.diagnostics,
  ]
}

resource "azurerm_role_assignment" "table_reader" {
  scope                = data.azurerm_storage_account.this.id
  role_definition_name = "Storage Table Data Reader"
  principal_id         = var.user_assigned_identity_principal_id
}

resource "azurerm_monitor_metric_alert" "diagnostics" {
  name                = "webhook-synthetic-health"
  resource_group_name = var.resource_group_name
  scopes              = [data.azurerm_application_insights.this.id]
  description         = "Webhook diagnostics synthetic health check failed"
  severity            = 1
  frequency           = "PT5M"
  window_size         = "PT15M"
  auto_mitigate       = true
  enabled             = true

  criteria {
    aggregation            = "Average"
    metric_name            = "webhook.synthetic.health"
    metric_namespace       = "azure.applicationinsights"
    operator               = "LessThan"
    threshold              = 1
    skip_metric_validation = true

  }

  dynamic "action" {
    for_each = var.application_insights_action_group_ids

    content {
      action_group_id = action.value
    }
  }

  depends_on = [azurerm_container_app_job.this]
  tags       = var.tags
}

resource "azurerm_monitor_metric_alert" "job_failure" {
  name                = "${var.prefix}-webhook-synthetic-job-failure"
  resource_group_name = var.resource_group_name
  scopes              = [azurerm_container_app_job.this.id]
  description         = "The webhook synthetic monitoring job failed to execute."
  severity            = 1
  frequency           = "PT5M"
  window_size         = "PT15M"

  criteria {
    metric_namespace = "Microsoft.App/jobs"
    metric_name      = "Executions"
    aggregation      = "Total"
    operator         = "GreaterThan"
    threshold        = 0

    dimension {
      name     = "state"
      operator = "Include"
      values   = ["Failed"]
    }
  }

  dynamic "action" {
    for_each = var.application_insights_action_group_ids

    content {
      action_group_id = action.value
    }
  }

  tags = var.tags
}
