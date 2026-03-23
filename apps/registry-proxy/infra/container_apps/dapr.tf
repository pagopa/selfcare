resource "azurerm_storage_container" "visura" {
  count                 = var.is_pnpg ? 0 : 1
  name                  = "visura"
  storage_account_id    = try(data.azurerm_storage_account.existing_logs_storage[0].id, null)
  container_access_type = "private"
}

resource "azurerm_container_app_environment_dapr_component" "blob_state" {
  count = var.is_pnpg ? 0 : 1

  name                         = "blobstorage-state"
  container_app_environment_id = local.cae_id
  component_type               = "state.azure.blobstorage"
  version                      = "v1"

  metadata {
    name  = "accountName"
    value = local.storage_account_name
  }

  metadata {
    name  = "containerName"
    value = try(azurerm_storage_container.visura[0].name, null)
  }

  metadata {
    name  = "azureClientId"
    value = data.azurerm_user_assigned_identity.cae_identity.client_id
  }

  scopes = [data.azurerm_container_app.ca[0].dapr[0].app_id]

  lifecycle {
    prevent_destroy = false
  }
}