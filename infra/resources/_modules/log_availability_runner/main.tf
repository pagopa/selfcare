locals {
  project     = "selc-${var.env_short}"
  stream_name = "Custom-SelcAvailability_CL"

  app_settings = [
    {
      name  = "AVAILABILITY_ENVIRONMENT"
      value = var.environment
    },
    {
      name  = "AVAILABILITY_SOURCE_RETENTION_DAYS"
      value = "90"
    },
    {
      name  = "LOG_ANALYTICS_WORKSPACE_ID"
      value = var.log_analytics_workspace_guid
    },
    {
      name  = "APPLICATION_GATEWAY_RESOURCE_ID"
      value = var.application_gateway_resource_id
    },
    {
      name  = "AVAILABILITY_STORAGE_ACCOUNT_NAME"
      value = var.storage_account_name
    },
    {
      name  = "LOGS_INGESTION_ENDPOINT"
      value = azurerm_monitor_data_collection_endpoint.availability.logs_ingestion_endpoint
    },
    {
      name  = "DCR_IMMUTABLE_ID"
      value = azurerm_monitor_data_collection_rule.availability.immutable_id
    }
  ]
}

resource "azurerm_monitor_data_collection_endpoint" "availability" {
  name                          = "${local.project}-availability-dce"
  resource_group_name           = var.monitor_resource_group_name
  location                      = var.location
  public_network_access_enabled = true
  tags                          = var.tags
}

resource "azurerm_monitor_data_collection_rule" "availability" {
  name                        = "${local.project}-availability-dcr"
  resource_group_name         = var.monitor_resource_group_name
  location                    = var.location
  data_collection_endpoint_id = azurerm_monitor_data_collection_endpoint.availability.id
  description                 = "Ingest daily Selfcare API availability records."
  tags                        = var.tags

  destinations {
    log_analytics {
      workspace_resource_id = var.log_analytics_workspace_resource_id
      name                  = "availability-workspace"
    }
  }

  stream_declaration {
    stream_name = local.stream_name

    column {
      name = "TimeGenerated"
      type = "datetime"
    }
    column {
      name = "ReferenceDate"
      type = "datetime"
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
      type = "datetime"
    }
  }

  data_flow {
    streams       = [local.stream_name]
    destinations  = ["availability-workspace"]
    output_stream = local.stream_name
  }
}

module "scheduled_job" {
  source = "../container_app_job"

  env_short                      = var.env_short
  resource_group_name            = var.container_app_resource_group_name
  container_app                  = var.container_app
  container_app_name             = "${local.project}-availability-daily"
  container_app_environment_name = var.container_app_environment_name
  image_name                     = "selfcare-log-availability-runner"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings
  secrets_names                  = {}
  key_vault_resource_group_name  = var.key_vault_resource_group_name
  key_vault_name                 = var.key_vault_name
  environment_identity_enabled   = false
  tags                           = var.tags

  schedule_trigger_config = [{
    cron_expression          = "0 1,2 * * *"
    parallelism              = 1
    replica_completion_count = 1
  }]

  replica_timeout_in_seconds = 28800
  replica_retry_limit        = 1
}

resource "azurerm_role_assignment" "workspace_reader" {
  scope                = var.log_analytics_workspace_resource_id
  role_definition_name = "Log Analytics Reader"
  principal_id         = module.scheduled_job.system_assigned_identity_principal_id
}

# Created through the management plane, so it works with public network access disabled.
resource "azurerm_storage_table" "availability" {
  name               = "SelcAvailability"
  storage_account_id = var.storage_account_id
}

# Scoped to the SelcAvailability table only: the account also hosts other tables.
resource "azurerm_role_assignment" "storage_table_contributor" {
  scope                = azurerm_storage_table.availability.id
  role_definition_name = "Storage Table Data Contributor"
  principal_id         = module.scheduled_job.system_assigned_identity_principal_id
}

resource "azurerm_role_assignment" "dcr_metrics_publisher" {
  scope                = azurerm_monitor_data_collection_rule.availability.id
  role_definition_name = "Monitoring Metrics Publisher"
  principal_id         = module.scheduled_job.system_assigned_identity_principal_id
}

data "azurerm_monitor_action_group" "availability_failure" {
  name                = var.environment == "PROD" ? "selcperror" : var.environment == "UAT" ? "selcuat" : "selcdev"
  resource_group_name = var.monitor_resource_group_name
}

resource "azurerm_monitor_metric_alert" "availability_job_failure" {
  name                = "${local.project}-availability-job-failure"
  resource_group_name = var.monitor_resource_group_name
  scopes              = [module.scheduled_job.container_app_job_id]
  description         = "Daily API availability job execution failed (${var.environment}). Inspect execution logs for the reference date and failure stage."
  severity            = 2
  enabled             = true
  auto_mitigate       = true
  frequency           = "PT5M"
  window_size         = "PT15M"
  tags                = var.tags

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

    # Keep separate failed executions visible even while an earlier alert is active.
    dimension {
      name     = "executionName"
      operator = "Include"
      values   = ["*"]
    }
  }

  action {
    action_group_id = data.azurerm_monitor_action_group.availability_failure.id
  }
}
