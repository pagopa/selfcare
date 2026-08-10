locals {
  storage_account_name = "${replace(var.prefix, "-", "")}synthmon"
  availability_name    = "synthetic-webhook-diagnostics"
  run_location         = "private"
}

data "azurerm_application_insights" "this" {
  name                = var.application_insight_name
  resource_group_name = var.application_insight_rg_name
}

resource "azurerm_storage_account" "this" {
  name                            = local.storage_account_name
  resource_group_name             = var.resource_group_name
  location                        = var.location
  account_tier                    = "Standard"
  account_replication_type        = "ZRS"
  account_kind                    = "StorageV2"
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false

  blob_properties {
    delete_retention_policy {
      days = 1
    }
  }

  tags = var.tags

  lifecycle {
    precondition {
      condition     = length(local.storage_account_name) <= 24
      error_message = "The synthetic monitoring storage account name must not exceed 24 characters."
    }
  }
}

resource "azurerm_storage_table" "configuration" {
  name                 = "monitoringconfiguration"
  storage_account_name = azurerm_storage_account.this.name
}

resource "azurerm_storage_table_entity" "diagnostics" {
  storage_table_id = azurerm_storage_table.configuration.id
  partition_key    = "webhook-diagnostics"
  row_key          = local.run_location

  entity = {
    url                 = var.diagnostics_url
    type                = local.run_location
    checkCertificate    = "false"
    alertEnabled        = "true"
    method              = "GET"
    domain              = ""
    expectedCodes       = jsonencode(["200"])
    durationLimit       = "5000"
    headers             = null
    body                = null
    tags                = jsonencode({ description = "Webhook Storage Queue and outbox diagnostics" })
    bodyCompareStrategy = null
    expectedBody        = null
  }
}

resource "azurerm_container_app_job" "this" {
  name                         = "${var.prefix}-monitoring-app-job"
  resource_group_name          = var.resource_group_name
  location                     = var.location
  container_app_environment_id = var.container_app_environment_id

  identity {
    type = "SystemAssigned"
  }

  schedule_trigger_config {
    cron_expression          = var.cron_scheduling
    parallelism              = 1
    replica_completion_count = 1
  }

  workload_profile_name = "Consumption"

  template {
    container {
      name   = "synthetic-monitoring"
      image  = "ghcr.io/pagopa/azure-synthetic-monitoring:v1.10.0@sha256:1686c4a719dc1a3c270f98f527ebc34179764ddf53ee3089febcb26df7a2d71d"
      cpu    = 0.25
      memory = "0.5Gi"

      env {
        name  = "APP_INSIGHT_CONNECTION_STRING"
        value = data.azurerm_application_insights.this.connection_string
      }
      env {
        name  = "STORAGE_ACCOUNT_NAME"
        value = azurerm_storage_account.this.name
      }
      env {
        name  = "STORAGE_ACCOUNT_KEY"
        value = azurerm_storage_account.this.primary_access_key
      }
      env {
        name  = "STORAGE_ACCOUNT_TABLE_NAME"
        value = azurerm_storage_table.configuration.name
      }
      env {
        name  = "AVAILABILITY_PREFIX"
        value = "synthetic"
      }
      env {
        name  = "HTTP_CLIENT_TIMEOUT"
        value = "30000"
      }
      env {
        name  = "LOCATION"
        value = var.location
      }
      env {
        name  = "CERT_VALIDITY_RANGE_DAYS"
        value = "7"
      }
    }
  }

  replica_retry_limit        = 1
  replica_timeout_in_seconds = 300

  tags = var.tags
}

resource "azurerm_monitor_metric_alert" "diagnostics" {
  name                = "availability-webhook-diagnostics-private"
  resource_group_name = var.resource_group_name
  scopes              = [data.azurerm_application_insights.this.id]
  description         = "Webhook diagnostics availability from the private Container Apps environment degraded"
  severity            = 1
  frequency           = "PT5M"
  auto_mitigate       = true
  enabled             = true

  criteria {
    aggregation      = "Average"
    metric_name      = "availabilityResults/availabilityPercentage"
    metric_namespace = "microsoft.insights/components"
    operator         = "LessThan"
    threshold        = 100

    dimension {
      name     = "availabilityResult/name"
      operator = "Include"
      values   = [local.availability_name]
    }

    dimension {
      name     = "availabilityResult/location"
      operator = "Include"
      values   = [local.run_location]
    }
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

resource "azurerm_monitor_metric_alert" "job" {
  name                = "availability-webhook-synthetic-monitoring-job"
  resource_group_name = var.resource_group_name
  scopes              = [data.azurerm_application_insights.this.id]
  description         = "Webhook synthetic monitoring job stopped publishing availability results"
  severity            = 1
  frequency           = "PT5M"
  auto_mitigate       = true
  enabled             = true

  criteria {
    aggregation      = "Average"
    metric_name      = "availabilityResults/availabilityPercentage"
    metric_namespace = "microsoft.insights/components"
    operator         = "LessThan"
    threshold        = 100

    dimension {
      name     = "availabilityResult/name"
      operator = "Include"
      values   = ["synthetic-monitoring-function"]
    }

    dimension {
      name     = "availabilityResult/location"
      operator = "Include"
      values   = [var.location]
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
