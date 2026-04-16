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

data "azurerm_resource_group" "nat_rg" {
  name = "${local.project}-nat-rg"
}

data "azurerm_nat_gateway" "nat_gateway" {
  name                = "${local.project}-nat_gw"
  resource_group_name = data.azurerm_resource_group.nat_rg.name
}