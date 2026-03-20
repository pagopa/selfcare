###############################################################################
# Storage
###############################################################################

data "azurerm_resource_group" "namirial_sign_contracts_storage_rg" {
  name = "${local.project}-contracts-storage-rg"
}

data "azurerm_container_app_environment" "namirial_sign_container_app_environment" {
  name                = local.container_app_environment_name
  resource_group_name = local.ca_resource_group_name
}

data "azurerm_private_dns_zone" "namirial_sign_private_azurecontainerapps_io" {
  name                = "azurecontainerapps.io"
  resource_group_name = local.resource_group_name_vnet
}

data "azurerm_key_vault_secret" "namirial_sign_hub_docker_user" {
  name         = "hub-docker-user"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "namirial_sign_hub_docker_pwd" {
  name         = "hub-docker-pwd"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_storage_account" "namirial_sign_storage_account" {
  name                            = replace(format("%s-namirial-sws-st", local.project), "-", "")
  resource_group_name             = data.azurerm_resource_group.namirial_sign_contracts_storage_rg.name
  location                        = data.azurerm_resource_group.namirial_sign_contracts_storage_rg.location
  min_tls_version                 = "TLS1_2"
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  allow_nested_items_to_be_public = false

  tags = local.tags
}

resource "azurerm_storage_share" "namirial_sign_storage_share" {
  name                 = "${local.project}-namirial-sws-share"
  storage_account_name = azurerm_storage_account.namirial_sign_storage_account.name
  quota                = 1
}

###############################################################################
# Container App
###############################################################################

locals {
  namirial_sign_container_app = {
    min_replicas = 1
    max_replicas = 1
    scale_rules  = []
    cpu          = 0.5
    memory       = "1Gi"
  }

  app_settings_namirial_sign = [
    {
      name  = "SPRINGDOC_API_DOCS_ENABLED"
      value = "true"
    }
  ]
}

resource "azapi_resource" "namirial_sign_storage_env" {
  type      = "Microsoft.App/managedEnvironments/storages@2023-05-01"
  name      = "${local.project}-namirial-sws-se"
  parent_id = data.azurerm_container_app_environment.namirial_sign_container_app_environment.id

  body = jsonencode({
    properties = {
      azureFile = {
        accountName = azurerm_storage_account.namirial_sign_storage_account.name
        shareName   = azurerm_storage_share.namirial_sign_storage_share.name
        accessMode  = "ReadWrite"
        accountKey  = azurerm_storage_account.namirial_sign_storage_account.primary_access_key
      }
    }
  })
}

resource "azapi_resource" "namirial_sign_container_app" {
  type      = "Microsoft.App/containerApps@2023-05-01"
  name      = "${local.project}-namirial-sws-ca"
  location  = data.azurerm_resource_group.namirial_sign_contracts_storage_rg.location
  parent_id = data.azurerm_resource_group.namirial_sign_contracts_storage_rg.id

  tags = local.tags

  identity {
    type = "SystemAssigned"
  }

  body = {
    properties = {
      configuration = {
        activeRevisionsMode = "Single"
        ingress = {
          allowInsecure = false
          external      = true
          targetPort    = 8080
          traffic = [
            {
              latestRevision = true
              label          = "latest"
              weight         = 100
            }
          ]
        }
        registries = [
          {
            server            = "index.docker.io"
            username          = data.azurerm_key_vault_secret.namirial_sign_hub_docker_user.value
            passwordSecretRef = "docker-password"
          }
        ]
        secrets = [
          {
            name  = "docker-password"
            value = data.azurerm_key_vault_secret.namirial_sign_hub_docker_pwd.value
          }
        ]
      }
      environmentId = data.azurerm_container_app_environment.namirial_sign_container_app_environment.id
      template = {
        containers = [
          {
            name  = "namirial-sws"
            image = "index.docker.io/namirial/sws:${local.namirial_sign_image_tag}"
            env   = local.app_settings_namirial_sign
            resources = {
              cpu    = local.namirial_sign_container_app.cpu
              memory = local.namirial_sign_container_app.memory
            }
            volumeMounts = [
              {
                mountPath  = "/opt/sws/custom"
                volumeName = "sws-storage"
              }
            ]
            probes = [
              {
                httpGet = {
                  path   = "/SignEngineWeb/rest/ready"
                  port   = 8080
                  scheme = "HTTP"
                }
                timeoutSeconds      = 4
                type                = "Liveness"
                failureThreshold    = 3
                initialDelaySeconds = 60
              },
              {
                httpGet = {
                  path   = "/SignEngineWeb/rest/ready"
                  port   = 8080
                  scheme = "HTTP"
                }
                timeoutSeconds      = 4
                type                = "Readiness"
                failureThreshold    = 30
                initialDelaySeconds = 30
              }
            ]
          }
        ]
        scale = {
          minReplicas = local.namirial_sign_container_app.min_replicas
          maxReplicas = local.namirial_sign_container_app.max_replicas
          rules       = local.namirial_sign_container_app.scale_rules
        }
        volumes = [
          {
            name        = "sws-storage"
            storageType = "AzureFile"
            storageName = azapi_resource.namirial_sign_storage_env.name
          }
        ]
      }
      workloadProfileName = "Consumption"
    }
  }
}

resource "azurerm_key_vault_access_policy" "namirial_sign_keyvault_containerapp_access_policy" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azapi_resource.namirial_sign_container_app.identity[0].principal_id

  secret_permissions = [
    "Get"
  ]
}

resource "azurerm_private_dns_a_record" "namirial_sign_private_dns_record_a_azurecontainerapps_io" {
  name = "${azapi_resource.namirial_sign_container_app.name}.${trimsuffix(data.azurerm_container_app_environment.namirial_sign_container_app_environment.default_domain, ".azurecontainerapps.io")}"

  zone_name           = data.azurerm_private_dns_zone.namirial_sign_private_azurecontainerapps_io.name
  resource_group_name = local.resource_group_name_vnet
  ttl                 = 3600
  records             = [data.azurerm_container_app_environment.namirial_sign_container_app_environment.static_ip_address]
}
