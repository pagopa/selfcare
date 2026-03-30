data "azurerm_virtual_network" "vnet_selc" {
  name                = "${local.project}-vnet"
  resource_group_name = local.resource_group_name_vnet
}

data "azurerm_key_vault" "key_vault" {
  name                = local.key_vault_name
  resource_group_name = local.key_vault_resource_group_name
}

data "azurerm_subscription" "current" {}

data "azurerm_client_config" "current" {}
