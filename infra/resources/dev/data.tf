data "azurerm_virtual_network" "vnet_selc" {
  name                = "${local.project}-vnet"
  resource_group_name = local.resource_group_name_vnet
}

# data "azurerm_linux_function_app" "onboarding_fn" {
#   name                = local.function_name
#   resource_group_name = "${local.function_name}-rg"
# }

data "azurerm_key_vault" "key_vault" {
  resource_group_name = local.key_vault_resource_group_name
  name                = local.key_vault_name
}
