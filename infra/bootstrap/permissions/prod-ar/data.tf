data "azurerm_key_vault" "key_vault" {
  name                = "${local.project}-kv"
  resource_group_name = "${local.project}-sec-rg"
}

data "azurerm_key_vault" "key_vault_pnpg" {
  name                = "${local.project}-pnpg-kv"
  resource_group_name = "${local.project}-pnpg-sec-rg"
}

data "azurerm_resource_group" "webhook_storage_rg" {
  name = "${local.prefix}-${local.env_short}-webhook-storage-rg"
}

data "azuread_group" "adgroup_developers" {
  display_name = "${local.prefix}-${local.env_short}-adgroup-developers"
}

data "azuread_group" "adgroup_admin" {
  display_name = "${local.prefix}-${local.env_short}-adgroup-admin"
}
