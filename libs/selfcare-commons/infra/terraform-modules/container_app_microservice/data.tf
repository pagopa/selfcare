data "azurerm_resource_group" "resource_group_app" {
  name = local.resource_group_name
}

data "azurerm_key_vault" "key_vault" {
  resource_group_name = local.key_vault_resource_group_name
  name                = local.key_vault_name
}

data "azurerm_key_vault_secrets" "key_vault_secrets" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "keyvault_secret" {
  for_each     = toset(data.azurerm_key_vault_secrets.key_vault_secrets.names)
  name         = each.key
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_container_app_environment" "container_app_environment" {
  resource_group_name = data.azurerm_resource_group.resource_group_app.name
  name                = local.container_app_environment_name
}

data "azurerm_resource_group" "rg_vnet" {
  name = local.vnet_name
}

data "azurerm_private_dns_zone" "private_azurecontainerapps_io" {
  name                = local.container_app_environment_dns_zone_name
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
}
