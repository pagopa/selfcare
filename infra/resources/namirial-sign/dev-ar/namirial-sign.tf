###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-ar"
}

# ###############################################################################
# # Namirial Sign Web Service
# ###############################################################################
module "namirial_sign" {
  source = "../../_modules/namirial_sws"

  prefix           = module.local.config.env_short
  env_short        = module.local.config.env_short
  cae_name         = module.local.config.container_app_environment_name
  tags             = module.local.config.tags
  enable_sws       = true
  enable_ca_sws    = true
  suffix_increment = "-002"
  environment_variables = {
    SPRINGDOC_API_DOCS_ENABLED = true
  }
  container_config = {
    cpu    = 0.5
    memory = 1
  }
  container_app = {
    min_replicas = 1
    max_replicas = 1
    scale_rules  = []
    cpu          = 0.5
    memory       = "1Gi"
  }
  app_settings = [
    {
      name  = "SPRINGDOC_API_DOCS_ENABLED",
      value = true
    }
  ]
}

# ###############################################################################
# # Storage
# ###############################################################################

# data "azurerm_resource_group" "namirial_sws_storage_rg" {
#   name = "${module.local.config.project}-contracts-storage-rg"
# }

# data "azurerm_container_app_environment" "namirial_sws_container_app_environment" {
#   name                = module.local.config.container_app_environment_name
#   resource_group_name = module.local.config.ca_resource_group_name
# }

# data "azurerm_private_dns_zone" "namirial_sws_private_azurecontainerapps_io" {
#   name                = "azurecontainerapps.io"
#   resource_group_name = module.local.config.resource_group_name_vnet
# }

# data "azurerm_key_vault_secret" "namirial_sws_hub_docker_user" {
#   name         = "hub-docker-user"
#   key_vault_id = data.azurerm_key_vault.key_vault.id
# }

# data "azurerm_key_vault_secret" "namirial_sws_hub_docker_pwd" {
#   name         = "hub-docker-pwd"
#   key_vault_id = data.azurerm_key_vault.key_vault.id
# }

# resource "azurerm_storage_account" "namirial_sws_storage_account" {
#   name                            = replace(format("%s-namirial-sws-st", module.local.config.project), "-", "")
#   resource_group_name             = data.azurerm_resource_group.namirial_sws_storage_rg.name
#   location                        = data.azurerm_resource_group.namirial_sws_storage_rg.location
#   min_tls_version                 = "TLS1_2"
#   account_tier                    = "Standard"
#   account_replication_type        = "LRS"
#   allow_nested_items_to_be_public = false

#   tags = module.local.config.tags
# }

# resource "azurerm_storage_share" "namirial_sws_storage_share" {
#   name                 = "${module.local.config.project}-namirial-sws-share"
#   storage_account_name = azurerm_storage_account.namirial_sws_storage_account.name
#   quota                = 1
# }

# ###############################################################################
# # Container App
# ###############################################################################

# locals {
#   image_tag = "3.0.11" #

#   namirial_sws_container_app = {
#     min_replicas = 1
#     max_replicas = 1
#     scale_rules  = []
#     cpu          = 0.5
#     memory       = "1Gi"
#   }

#   app_settings_namirial_sign = [
#     {
#       name  = "SPRINGDOC_API_DOCS_ENABLED"
#       value = "true"
#     }
#   ]
# }

# resource "azurerm_container_app_environment_storage" "namirial_sws_storage_env" {
#   name                         = "${module.local.config.project}-namirial-sws-se"
#   container_app_environment_id = data.azurerm_container_app_environment.namirial_sws_container_app_environment.id
#   account_name                 = azurerm_storage_account.namirial_sws_storage_account.name
#   share_name                   = azurerm_storage_share.namirial_sws_storage_share.name
#   access_key                   = azurerm_storage_account.namirial_sws_storage_account.primary_access_key
#   access_mode                  = "ReadWrite"
# }

# resource "azurerm_container_app" "namirial_sws_container_app" {
#   name                         = "${module.local.config.project}-namirial-sws-ca"
#   resource_group_name          = data.azurerm_resource_group.namirial_sws_storage_rg.name
#   container_app_environment_id = data.azurerm_container_app_environment.namirial_sws_container_app_environment.id
#   revision_mode                = "Single"
#   workload_profile_name        = "Consumption"

#   tags = module.local.config.tags

#   identity {
#     type = "SystemAssigned"
#   }

#   ingress {
#     allow_insecure_connections = false
#     external_enabled           = true
#     target_port                = 8080
#     transport                  = "auto"

#     traffic_weight {
#       latest_revision = true
#       percentage      = 100
#     }
#   }

#   registry {
#     server               = "index.docker.io"
#     username             = data.azurerm_key_vault_secret.namirial_sws_hub_docker_user.value
#     password_secret_name = "docker-password"
#   }

#   secret {
#     name  = "docker-password"
#     value = data.azurerm_key_vault_secret.namirial_sws_hub_docker_pwd.value
#   }

#   template {
#     min_replicas = module.local.config.namirial_sws_container_app.min_replicas
#     max_replicas = module.local.config.namirial_sws_container_app.max_replicas

#     container {
#       name   = "namirial-sws"
#       image  = "index.docker.io/namirial/sws:${local.image_tag}"
#       cpu    = module.local.config.namirial_sws_container_app.cpu
#       memory = module.local.config.namirial_sws_container_app.memory

#       # Gestione delle variabili d'ambiente tramite dynamic block
#       dynamic "env" {
#         for_each = module.local.config.app_settings_namirial_sign
#         content {
#           name        = env.value.name
#           value       = lookup(env.value, "value", null)
#           secret_name = lookup(env.value, "secret_name", null)
#         }
#       }

#       volume_mounts {
#         name = "sws-storage"
#         path = "/opt/sws/custom"
#       }

#       liveness_probe {
#         path                    = "/SignEngineWeb/rest/ready"
#         port                    = 8080
#         transport               = "HTTP"
#         timeout                 = 4
#         failure_count_threshold = 3
#         initial_delay           = 60
#       }

#       readiness_probe {
#         path                    = "/SignEngineWeb/rest/ready"
#         port                    = 8080
#         transport               = "HTTP"
#         timeout                 = 4
#         failure_count_threshold = 30
#         initial_delay           = 30
#       }
#     }

#     volume {
#       name         = "sws-storage"
#       storage_type = "AzureFile"
#       storage_name = azurerm_container_app_environment_storage.namirial_sws_storage_env.name
#     }
#   }
# }

# resource "azurerm_key_vault_access_policy" "namirial_sign_keyvault_containerapp_access_policy" {
#   key_vault_id = data.azurerm_key_vault.key_vault.id
#   tenant_id    = data.azurerm_client_config.current.tenant_id
#   object_id    = azurerm_container_app.namirial_sws_container_app.identity[0].principal_id

#   secret_permissions = [
#     "Get"
#   ]
# }

# resource "azurerm_private_dns_a_record" "namirial_sws_private_dns_record_a_azurecontainerapps_io" {
#   name = "${azurerm_container_app.namirial_sws_container_app.name}.${trimsuffix(data.azurerm_container_app_environment.namirial_sws_container_app_environment.default_domain, ".azurecontainerapps.io")}"

#   zone_name           = data.azurerm_private_dns_zone.namirial_sws_private_azurecontainerapps_io.name
#   resource_group_name = module.local.config.resource_group_name_vnet
#   ttl                 = 3600
#   records             = [data.azurerm_container_app_environment.namirial_sws_container_app_environment.static_ip_address]
# }
