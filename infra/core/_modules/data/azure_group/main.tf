locals {
  project = "${var.prefix}-${var.env_short}"
}

# data "azurerm_subscription" "current" {}
# data "azurerm_client_config" "current" {}

# Azure AD Groups
data "azuread_group" "adgroup_admin" {
  display_name = "${local.project}-adgroup-admin"
}

data "azuread_group" "adgroup_developers" {
  display_name = "${local.project}-adgroup-developers"
}

data "azuread_group" "adgroup_externals" {
  display_name = "${local.project}-adgroup-externals"
}

data "azuread_group" "adgroup_security" {
  display_name = "${local.project}-adgroup-security"
}
