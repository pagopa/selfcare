data "azurerm_resource_group" "rg_contracts_storage" {
  name = "${local.project}-contracts-storage-rg"
}

data "azurerm_key_vault" "key_vault" {
  name                = "${local.project}-kv"
  resource_group_name = "${local.project}-sec-rg"
}

data "azurerm_key_vault_secret" "hub_docker_user" {
  name         = "hub-docker-user"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "hub_docker_pwd" {
  name         = "hub-docker-pwd"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secrets" "key_vault_secrets" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "keyvault_secret" {
  for_each     = toset(data.azurerm_key_vault_secrets.key_vault_secrets.names)
  name         = each.key
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_log_analytics_workspace" "log_analytics" {
  name                = "${local.project}-law"
  resource_group_name = "${local.project}-monitor-rg"
}

data "azurerm_virtual_network" "vnet_selc" {
  name                = "${local.project}-vnet"
  resource_group_name = "${local.project}-vnet-rg"
}

data "azurerm_container_app_environment" "container_app_environment" {
  resource_group_name = local.ca_resource_group_name
  name                = local.container_app_environment_name
}

data "azurerm_resource_group" "rg_vnet" {
  name = local.vnet_name
}

data "azurerm_private_dns_zone" "private_azurecontainerapps_io" {
  name                = local.container_app_environment_dns_zone_name
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
}