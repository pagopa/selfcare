data "azurerm_subscription" "current" {}

data "azurerm_resource_group" "rg_vnet" {
  name = "${var.prefix}-${var.env_short}-vnet-rg"
}

data "azurerm_virtual_network" "vnet" {
  name                = "${var.prefix}-${var.env_short}-vnet"
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
}

data "azurerm_key_vault" "key_vault" {
  resource_group_name = var.key_vault_resource_group_name
  name                = var.key_vault_name
}

data "azuread_group" "adgroup_admin" {
  display_name = "${var.prefix}-${var.env_short}-adgroup-admin"
}

data "azuread_group" "adgroup_developers" {
  display_name = "${var.prefix}-${var.env_short}-adgroup-developers"
}

data "azurerm_user_assigned_identity" "managed_identity_infra_ci" {
  name                = "${var.prefix}-${var.env_short}-ms-github-ci-identity"
  resource_group_name = "${var.prefix}-${var.env_short}-identity-rg"
}

data "azurerm_user_assigned_identity" "managed_identity_infra_cd" {
  name                = "${var.prefix}-${var.env_short}-ms-github-cd-identity"
  resource_group_name = "${var.prefix}-${var.env_short}-identity-rg"
}
