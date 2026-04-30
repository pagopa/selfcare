resource "azurerm_container_app_job" "container_app_job" {
  name                         = local.app_name
  container_app_environment_id = data.azurerm_container_app_environment.container_app_environment.id
  resource_group_name          = data.azurerm_resource_group.resource_group_app.name
  workload_profile_name        = var.workload_profile_name
  tags                         = var.tags

  location                     = data.azurerm_resource_group.resource_group_app.location
  replica_timeout_in_seconds   = var.replica_timeout_in_seconds
  replica_retry_limit          = var.replica_retry_limit

  # Managed Identity
  identity {
    type = "SystemAssigned, UserAssigned"
    identity_ids = [
      data.azurerm_user_assigned_identity.cae_identity.id
    ]
  }

  # Secrets configuration
  dynamic "secret" {
    for_each = local.secrets
    content {
      name                = secret.value.name
      key_vault_secret_id = secret.value.key_vault_secret_name
      identity            = data.azurerm_user_assigned_identity.cae_identity.id
    }
  }

  # Trigger configuration
  dynamic "schedule_trigger_config" {
    for_each = var.schedule_trigger_config

    content {
      cron_expression          = schedule_trigger_config.value.cron_expression
      parallelism              = schedule_trigger_config.value.parallelism
      replica_completion_count = schedule_trigger_config.value.replica_completion_count
    }
  }

  # Manual trigger configuration
  dynamic "manual_trigger_config" {
    for_each = var.manual_trigger_config

    content {
      parallelism              = manual_trigger_config.value.parallelism
      replica_completion_count = manual_trigger_config.value.replica_completion_count
    }
  }

  # Template configuration
  template {
    # Container configuration
    container {
      name   = var.container_app_name
      image  = "ghcr.io/pagopa/${var.image_name}:${var.image_tag}"
      cpu    = var.container_app.cpu
      memory = var.container_app.memory

      # Environment variables from app_settings
      dynamic "env" {
        for_each = var.app_settings
        content {
          name  = env.value.name
          value = env.value.value
        }
      }

      # Environment variables from secrets
      dynamic "env" {
        for_each = local.secrets_env

        content {
          name        = env.value.name
          secret_name = env.value.secretRef
        }
      }

    }
  }
}
