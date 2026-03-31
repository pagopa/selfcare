data "azurerm_container_app_environment" "cae" {
  name                = module.local.config.container_app_environment_name
  resource_group_name = module.local.config.ca_resource_group_name
}

data "azurerm_container_app" "ca" {
  name                = module.local.config.ca_name
  resource_group_name = module.local.config.ca_resource_group_name
}

data "azurerm_storage_account" "existing_logs_storage" {
  name                = module.local.config.storage_logs
  resource_group_name = module.local.config.existing_logs_rg
}

# data "azurerm_key_vault_secret" "logs_storage_access_key" {
#   name         = "logs-storage-access-key"
#   key_vault_id = data.azurerm_key_vault.key_vault.id
# }
