data "azurerm_container_app_environment" "cae" {
  count               = var.is_pnpg ? 0 : 1
  name                = local.container_app_environment_name
  resource_group_name = var.cae_rg_name
}

data "azurerm_container_app" "ca" {
  count               = var.is_pnpg ? 0 : 1
  name                = var.ca_name
  resource_group_name = var.ca_rg_name
}

data "azurerm_storage_account" "existing_logs_storage" {
  count               = var.is_pnpg ? 0 : 1
  name                = var.storage_logs
  resource_group_name = var.existing_logs_rg
}

data "azurerm_key_vault" "key_vault" {
  count               = var.is_pnpg ? 0 : 1
  name                = var.key_vault_name
  resource_group_name = var.key_vault_resource_group_name
}

data "azurerm_key_vault_secret" "logs_storage_access_key" {
  count       = var.is_pnpg ? 0 : 1
  name        = "logs-storage-access-key"
  key_vault_id = try(data.azurerm_key_vault.key_vault[0].id, null)
}

locals {
  cae_id               = try(data.azurerm_container_app_environment.cae[0].id, null)
  container_app_id     = try(data.azurerm_container_app.ca[0].id, null)
  storage_account_id   = try(data.azurerm_storage_account.existing_logs_storage[0].id, null)
  storage_account_name = try(data.azurerm_storage_account.existing_logs_storage[0].name, null)
  key_vault_id         = try(data.azurerm_key_vault.key_vault[0].id, null)
  logs_storage_key     = try(data.azurerm_key_vault_secret.logs_storage_access_key[0].value, null)
}
