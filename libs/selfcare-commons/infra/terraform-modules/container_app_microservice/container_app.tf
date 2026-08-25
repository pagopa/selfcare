resource "azurerm_container_app" "container_app" {
  name                         = local.app_name
  container_app_environment_id = data.azurerm_container_app_environment.container_app_environment.id
  resource_group_name          = data.azurerm_resource_group.resource_group_app.name
  revision_mode               = "Single"
  workload_profile_name       = var.workload_profile_name
  tags                        = var.tags

  # Managed Identity
  identity {
    type = "SystemAssigned, UserAssigned"
    identity_ids = [
      var.user_assigned_identity_id
    ]
  }

  # Secrets configuration
  dynamic "secret" {
    for_each = local.secrets
    content {
      name                = secret.value.name
      key_vault_secret_id = secret.value.key_vault_secret_name
      identity            = var.user_assigned_identity_id
    }
  }


  dynamic "dapr" {
    for_each = var.dapr_settings
    content {
      app_id       = dapr.value.app_id
      app_port     = dapr.value.app_port
      app_protocol = dapr.value.app_protocol
    }
  }
  
  # Ingress configuration
  ingress {
    allow_insecure_connections = false
    external_enabled          = true
    target_port              = 8080

    traffic_weight {
      latest_revision = true
      label          = "latest"
      percentage     = 100
    }
  }

  # Template configuration
  template {
    # Container configuration
    container {
      name   = local.container_name
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

      # Probes configuration
      dynamic "liveness_probe" {
        for_each = [for probe in var.probes : probe if probe.type == "Liveness"]
        content {
          transport = liveness_probe.value.httpGet.scheme
          port      = liveness_probe.value.httpGet.port
          path      = try(liveness_probe.value.httpGet.path, null)

          initial_delay           = try(liveness_probe.value.initialDelaySeconds, 0)
          interval_seconds        = try(liveness_probe.value.periodSeconds, 10)
          timeout                = try(liveness_probe.value.timeoutSeconds, 30)
          failure_count_threshold = try(liveness_probe.value.failureThreshold, 3)

          dynamic "header" {
            for_each = try(liveness_probe.value.httpHeaders, [])
            content {
              name  = header.value.name
              value = header.value.value
            }
          }
        }
      }

      dynamic "readiness_probe" {
        for_each = [for probe in var.probes : probe if probe.type == "Readiness"]
        content {
          transport = readiness_probe.value.httpGet.scheme
          port      = readiness_probe.value.httpGet.port
          path      = try(readiness_probe.value.httpGet.path, null)

          initial_delay           = try(readiness_probe.value.initialDelaySeconds, 0)
          interval_seconds        = try(readiness_probe.value.periodSeconds, 10)
          timeout                = try(readiness_probe.value.timeoutSeconds, 30)
          failure_count_threshold = try(readiness_probe.value.failureThreshold, 3)
          success_count_threshold = try(readiness_probe.value.successThreshold, 1)

          dynamic "header" {
            for_each = try(readiness_probe.value.httpHeaders, [])
            content {
              name  = header.value.name
              value = header.value.value
            }
          }
        }
      }

      dynamic "startup_probe" {
        for_each = [for probe in var.probes : probe if probe.type == "Startup"]
        content {
          transport = startup_probe.value.httpGet.scheme
          port      = startup_probe.value.httpGet.port
          path      = try(startup_probe.value.httpGet.path, null)

          initial_delay           = try(startup_probe.value.initialDelaySeconds, 0)
          interval_seconds        = try(startup_probe.value.periodSeconds, 10)
          timeout                = try(startup_probe.value.timeoutSeconds, 30)
          failure_count_threshold = try(startup_probe.value.failureThreshold, 3)

          dynamic "header" {
            for_each = try(startup_probe.value.httpHeaders, [])
            content {
              name  = header.value.name
              value = header.value.value
            }
          }
        }
      }
    }

    # Scaling configuration
    max_replicas = var.container_app.max_replicas
    min_replicas = var.container_app.min_replicas

    # Scale rules
    dynamic "azure_queue_scale_rule" {
      for_each = [for rule in var.container_app.scale_rules : rule if rule.type == "azure-queue"]
      content {
        name         = azure_queue_scale_rule.value.name
        queue_name   = azure_queue_scale_rule.value.metadata.queueName
        queue_length = azure_queue_scale_rule.value.metadata.queueLength

        authentication {
          secret_name       = azure_queue_scale_rule.value.auth[0].secretRef
          trigger_parameter = azure_queue_scale_rule.value.auth[0].triggerParameter
        }
      }
    }

    dynamic "http_scale_rule" {
      for_each = [for rule in var.container_app.scale_rules : rule if rule.custom.type == "http"]
      content {
        name                = http_scale_rule.value.name
        concurrent_requests = http_scale_rule.value.custom.metadata.concurrentRequests
      }
    }


    dynamic "custom_scale_rule" {
      for_each = [for rule in var.container_app.scale_rules : rule if !contains(["azure-queue", "http"], rule.custom.type)]
      content {
        name             = custom_scale_rule.value.name
        custom_rule_type = custom_scale_rule.value.custom.type
        metadata         = custom_scale_rule.value.custom.metadata

        dynamic "authentication" {
          for_each = try(custom_scale_rule.value.auth, [])
          content {
            secret_name       = authentication.value.secretRef
            trigger_parameter = authentication.value.triggerParameter
          }
        }
      }
    }
  }
}

# data "azurerm_key_vault_access_policy" "keyvault_containerapp_access_policy" {
#   name        = "${local.project}-keyvault-access-policy-containerapp"
# }

resource "azurerm_key_vault_access_policy" "keyvault_containerapp_access_policy" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.user_assigned_identity_principal_id

  secret_permissions = [
    "Get",
    "List"
  ]
}

# resource "azurerm_role_definition" "container_apps_action" {
#   name        = "SelfCare ContainerApp action"
#   scope       = azurerm_container_app.container_app.id
#   description = "Can read Container App secrets"

# permissions {
#     actions = [
#       "microsoft.app/containerApps/listSecrets/action",
#       "Microsoft.App/containerApps/read"
#     ]
#   }

#   assignable_scopes = [
#     azurerm_container_app.container_app.id
#   ]
# }

# data "azurerm_user_assigned_identity" "github_cd" {
#   name                = "${local.project}-infra-github-cd-identity"
#   resource_group_name = "${local.project}-identity-rg"
# }

# data "azurerm_user_assigned_identity" "github_ci" {
#   name                = "${local.project}-infra-github-ci-identity"
#   resource_group_name = "${local.project}-identity-rg"
# }

# resource "azurerm_role_assignment" "github_cd_role_assignment" {
#   scope              = data.azurerm_resource_group.resource_group_app.id
#   role_definition_id = azurerm_role_definition.container_apps_action.role_definition_resource_id
#   principal_id       = data.azurerm_user_assigned_identity.github_cd.principal_id
# }

# resource "azurerm_role_assignment" "github_ci_role_assignment" {
#   scope              = data.azurerm_resource_group.resource_group_app.id
#   role_definition_id = azurerm_role_definition.container_apps_action.role_definition_resource_id
#   principal_id       = data.azurerm_user_assigned_identity.github_ci.principal_id
# }
