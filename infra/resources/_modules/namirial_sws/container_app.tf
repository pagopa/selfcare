# resource "azapi_resource" "namirial_sws_storage_env" {
#   count     = var.enable_ca_sws ? 1 : 0
#   type      = "Microsoft.App/managedEnvironments/storages@2023-05-01"
#   name      = "${local.project}-namirial-sws-se"
#   parent_id = data.azurerm_container_app_environment.container_app_environment.id

#   body = jsonencode({
#     properties = {
#       azureFile = {
#         accountName = azurerm_storage_account.namirial_sws_storage_account[0].name
#         shareName   = azurerm_storage_share.namirial_sws_storage_share[0].name
#         accessMode  = "ReadWrite"
#         accountKey  = azurerm_storage_account.namirial_sws_storage_account[0].primary_access_key
#       }
#     }
#   })
# }

resource "azurerm_container_app_environment_storage" "namirial_sws_storage_env" {
count     = var.enable_ca_sws ? 1 : 0
  name                         =  "${local.project}-namirial-sws-se"
  container_app_environment_id = data.azurerm_container_app_environment.container_app_environment.id
  account_name                 = azurerm_storage_account.namirial_sws_storage_account[0].name
  share_name                   = azurerm_storage_share.namirial_sws_storage_share[0].name
  access_key                   = azurerm_storage_account.namirial_sws_storage_account[0].primary_access_key
  access_mode                  = "ReadWrite"
}

resource "azurerm_container_app" "namirial_container_app" {
  count                        = var.enable_ca_sws ? 1 : 0
  name                         = "${local.project}-namirial-sws-ca"
  resource_group_name          = data.azurerm_resource_group.rg_contracts_storage.name
  container_app_environment_id = data.azurerm_container_app_environment.container_app_environment.id
  revision_mode                = "Single"
  workload_profile_name        = var.workload_profile_name
  tags                         = var.tags

  identity {
    type = "SystemAssigned"
  }

  ingress {
    allow_insecure_connections = false
    external_enabled           = true
    target_port                = 8080

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  registry {
    server               = "index.docker.io"
    username             = data.azurerm_key_vault_secret.hub_docker_user.value
    password_secret_name = "docker-password"
  }

  # Segreto di Docker
  secret {
    name  = "docker-password"
    value = data.azurerm_key_vault_secret.hub_docker_pwd.value
  }

  # Segreti passati tramite local.secrets
  dynamic "secret" {
    for_each = local.secrets
    content {
      name  = secret.value.name
      value = secret.value.value
    }
  }

  template {
    min_replicas = var.container_app.min_replicas
    max_replicas = var.container_app.max_replicas

    volume {
      name         = "sws-storage"
      storage_type = "AzureFile"
      # storage_name = azapi_resource.namirial_sws_storage_env[0].name
      storage_name = azurerm_container_app_environment_storage.namirial_sws_storage_env[0].name
    }

    container {
      name   = "namirial-sws"
      image  = "index.docker.io/namirial/sws:3.0.0"
      cpu    = var.container_app.cpu
      memory = var.container_app.memory

      volume_mounts {
        name = "sws-storage"
        path = "/opt/sws/custom"
      }

      # Variabili d'ambiente concatenate
      dynamic "env" {
        for_each = concat(var.app_settings, local.secrets_env)
        content {
          name        = env.value.name
          value       = lookup(env.value, "value", null)
          # In azapi si usa 'secretRef', in azurerm si usa 'secret_name'
          secret_name = lookup(env.value, "secretRef", lookup(env.value, "secret_name", null))
        }
      }

      liveness_probe {
        port                    = 8080
        transport               = "HTTP"
        path                    = "/SignEngineWeb/rest/ready"
        timeout                 = 4
        failure_count_threshold = 3
        initial_delay           = 60
      }

      readiness_probe {
        port                    = 8080
        transport               = "HTTP"
        path                    = "/SignEngineWeb/rest/ready"
        timeout                 = 4
        failure_count_threshold = 30
        initial_delay           = 30
      }
    }

    # ATTENZIONE ALLE SCALE RULES:
    # In azurerm devono essere mappate nei blocchi specifici:
    # custom_scale_rule, http_scale_rule, tcp_scale_rule, ecc.
    # Ecco un esempio generico per le custom_scale_rule:
    dynamic "custom_scale_rule" {
      for_each = var.container_app.scale_rules != null ? var.container_app.scale_rules : []
      content {
        name             = custom_scale_rule.value.name
        custom_rule_type = try(custom_scale_rule.value.custom.type, custom_scale_rule.value.type)
        metadata         = try(custom_scale_rule.value.custom.metadata, custom_scale_rule.value.metadata)
      }
    }
  }
}

# resource "azapi_resource" "namirial_container_app" {
#   count     = var.enable_ca_sws ? 1 : 0
#   type      = "Microsoft.App/containerApps@2023-05-01"
#   name      = "${local.project}-namirial-sws-ca"
#   location  = data.azurerm_resource_group.rg_contracts_storage.location
#   parent_id = data.azurerm_resource_group.rg_contracts_storage.id

#   tags = var.tags

#   identity {
#     type = "SystemAssigned"
#   }

#   body = jsonencode({
#     properties = {
#       configuration = {
#         activeRevisionsMode = "Single"
#         ingress = {
#           allowInsecure = false
#           external      = true
#           traffic = [
#             {
#               latestRevision = true
#               label          = "latest"
#               weight         = 100
#             }
#           ]
#           targetPort = 8080
#         }

#         registries = [
#           {
#             server            = "index.docker.io"
#             username          = data.azurerm_key_vault_secret.hub_docker_user.value
#             passwordSecretRef = "docker-password"
#           }
#         ]
#         secrets = concat([
#           {
#             name  = "docker-password"
#             value = data.azurerm_key_vault_secret.hub_docker_pwd.value
#           }
#         ], local.secrets)
#       }
#       environmentId = data.azurerm_container_app_environment.container_app_environment.id
#       template = {
#         containers = [
#           {
#             env   = concat(var.app_settings, local.secrets_env)
#             image = "index.docker.io/namirial/sws:3.0.0"
#             name  = "namirial-sws"
#             resources = {
#               cpu    = var.container_app.cpu
#               memory = var.container_app.memory
#             }
#             volumeMounts = [
#               {
#                 mountPath  = "/opt/sws/custom"
#                 volumeName = "sws-storage"
#               }
#             ]
#             probes = [
#               {
#                 httpGet = {
#                   path   = "/SignEngineWeb/rest/ready"
#                   port   = 8080
#                   scheme = "HTTP"
#                 }
#                 timeoutSeconds      = 4
#                 type                = "Liveness"
#                 failureThreshold    = 3
#                 initialDelaySeconds = 60
#               },
#               {
#                 httpGet = {
#                   path   = "/SignEngineWeb/rest/ready"
#                   port   = 8080
#                   scheme = "HTTP"
#                 }
#                 timeoutSeconds      = 4
#                 type                = "Readiness"
#                 failureThreshold    = 30
#                 initialDelaySeconds = 30
#               }
#             ]
#           }
#         ]
#         scale = {
#           maxReplicas = var.container_app.max_replicas
#           minReplicas = var.container_app.min_replicas
#           rules       = var.container_app.scale_rules
#         }
#         volumes = [
#           {
#             name        = "sws-storage"
#             storageType = "AzureFile"
#             storageName = azapi_resource.namirial_sws_storage_env[0].name
#           }
#         ]
#       }
#       workloadProfileName = var.workload_profile_name
#     }
#   })
# }

resource "azurerm_key_vault_access_policy" "keyvault_containerapp_access_policy" {
  count        = var.enable_ca_sws ? 1 : 0
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_container_app.namirial_container_app[count.index].identity[0].principal_id

  secret_permissions = [
    "Get",
  ]
}
