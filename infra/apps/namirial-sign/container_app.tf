resource "azapi_resource" "namirial_sws_storage_env" {
  count     = var.enable_ca_sws ? 1 : 0
  type      = "Microsoft.App/managedEnvironments/storages@2023-05-01"
  name      = "${local.project}-namirial-sws-se"
  parent_id = data.azurerm_container_app_environment.container_app_environment.id

  body = jsonencode({
    properties = {
      azureFile = {
        accountName = azurerm_storage_account.namirial_sws_storage_account[0].name
        shareName   = azurerm_storage_share.namirial_sws_storage_share[0].name
        accessMode  = "ReadWrite"
        accountKey  = azurerm_storage_account.namirial_sws_storage_account[0].primary_access_key
      }
    }
  })
}

resource "azapi_resource" "namirial_container_app" {
  count     = var.enable_ca_sws ? 1 : 0
  type      = "Microsoft.App/containerApps@2023-05-01"
  name      = "${local.project}-namirial-sws-ca"
  location  = data.azurerm_resource_group.rg_contracts_storage.location
  parent_id = data.azurerm_resource_group.rg_contracts_storage.id

  tags = var.tags

  identity {
    type = "SystemAssigned"
  }

  body = jsonencode({
    properties = {
      configuration = {
        activeRevisionsMode = "Single"
        ingress = {
          allowInsecure = false
          external      = true
          traffic = [
            {
              latestRevision = true
              label          = "latest"
              weight         = 100
            }
          ]
          targetPort = 8080
        }

        registries = [
          {
            server            = "index.docker.io"
            username          = data.azurerm_key_vault_secret.hub_docker_user.value
            passwordSecretRef = "docker-password"
          }
        ]
        secrets = concat([
          {
            name  = "docker-password"
            value = data.azurerm_key_vault_secret.hub_docker_pwd.value
          }
        ], local.secrets)
      }
      environmentId = data.azurerm_container_app_environment.container_app_environment.id
      template = {
        containers = [
          {
            env   = concat(var.app_settings, local.secrets_env)
            image = "index.docker.io/namirial/sws:3.0.0"
            name  = "namirial-sws"
            resources = {
              cpu    = var.container_app.cpu
              memory = var.container_app.memory
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
          maxReplicas = var.container_app.max_replicas
          minReplicas = var.container_app.min_replicas
          rules       = var.container_app.scale_rules
        }
        volumes = [
          {
            name        = "sws-storage"
            storageType = "AzureFile"
            storageName = azapi_resource.namirial_sws_storage_env[0].name
          }
        ]
      }
      workloadProfileName = var.workload_profile_name
    }
  })
}

resource "azurerm_key_vault_access_policy" "keyvault_containerapp_access_policy" {
  count        = var.enable_ca_sws ? 1 : 0
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azapi_resource.namirial_container_app[count.index].identity[0].principal_id

  secret_permissions = [
    "Get",
  ]
}
