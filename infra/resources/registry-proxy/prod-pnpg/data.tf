# data "azurerm_virtual_network" "vnet_selc" {
#   name                = "${local.project}-vnet"
#   resource_group_name = local.resource_group_name_vnet
# }

# data "azurerm_key_vault" "key_vault" {
#   resource_group_name = local.key_vault_resource_group_name
#   name                = local.key_vault_name
# }

# data "azurerm_subscription" "current" {}

# data "azurerm_client_config" "current" {}

# data "azurerm_container_app_environment" "cae" {
#   name                = local.container_app_environment_name
#   resource_group_name = local.ca_resource_group_name
# }

# data "azurerm_container_app" "ca" {
#   name                = local.ca_name
#   resource_group_name = local.ca_resource_group_name
# }

# data "azurerm_storage_account" "existing_logs_storage" {
#   name                = local.storage_logs
#   resource_group_name = local.existing_logs_rg
# }

# data "azurerm_key_vault_secret" "logs_storage_access_key" {
#   name         = "logs-storage-access-key"
#   key_vault_id = data.azurerm_key_vault.key_vault.id
# }
