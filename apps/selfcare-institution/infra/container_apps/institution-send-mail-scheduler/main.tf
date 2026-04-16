terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "=3.111.0"
    }
  }

  backend "azurerm" {}
}

provider "azurerm" {
  features {}
}

data "azurerm_client_config" "current" {}

resource "azurerm_container_app_job" "container_app_job_institution_send_mail_scheduler" {
  name                         = local.container_app_name
  location                     = data.azurerm_resource_group.resource_group_app.location
  resource_group_name          = data.azurerm_resource_group.resource_group_app.name
  container_app_environment_id = data.azurerm_container_app_environment.container_app_environment.id
  workload_profile_name        = var.workload_profile_name

  replica_timeout_in_seconds = 28800
  replica_retry_limit        = 0 #we avoid to run more times causing multiple send mails

  dynamic "schedule_trigger_config" {
    for_each = var.schedule_trigger_config

    content {
      cron_expression          = schedule_trigger_config.value.cron_expression
      parallelism              = schedule_trigger_config.value.parallelism
      replica_completion_count = schedule_trigger_config.value.replica_completion_count
    }
  }

  dynamic "manual_trigger_config" {
    for_each = var.manual_trigger_config

    content {
      parallelism              = manual_trigger_config.value.parallelism
      replica_completion_count = manual_trigger_config.value.replica_completion_count
    }
  }

  identity {
    type = "SystemAssigned"
  }

  dynamic "secret" {
    for_each = var.secrets_names

    content {
      identity            = "System"
      name                = secret.value
      key_vault_secret_id = data.azurerm_key_vault_secret.keyvault_secret["${secret.value}"].id
    }
  }

  tags = var.tags

  template {
    container {
      image = "ghcr.io/pagopa/${local.image_name}:${var.image_tag}"
      name  = local.container_app_name
      readiness_probe {
        transport = "HTTP"
        port      = 5000
      }

      dynamic "env" {
        for_each = concat(var.app_settings, local.secrets_env)

        content {
          name        = env.value.name
          value       = contains(keys(env.value), "value") ? env.value.value : null
          secret_name = contains(keys(env.value), "secretRef") ? env.value.secretRef : null
        }
      }

      liveness_probe {
        transport = "HTTP"
        port      = 5000
        path      = "/health"

        header {
          name  = "Cache-Control"
          value = "no-cache"
        }

        initial_delay           = 5
        interval_seconds        = 20
        timeout                 = 2
        failure_count_threshold = 1
      }
      startup_probe {
        transport = "TCP"
        port      = 5000
      }

      cpu    = 0.5
      memory = "1Gi"
    }
  }
}

resource "azurerm_key_vault_access_policy" "keyvault_containerapp_access_policy" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_container_app_job.container_app_job_institution_send_mail_scheduler.identity[0].principal_id

  secret_permissions = [
    "Get",
  ]
}
