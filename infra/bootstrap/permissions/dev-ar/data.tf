data "azurerm_key_vault" "key_vault" {
  name                = "${local.project}-kv"
  resource_group_name = "${local.project}-sec-rg"
}

data "azurerm_key_vault" "key_vault_pnpg" {
  name                = "${local.project}-pnpg-kv"
  resource_group_name = "${local.project}-pnpg-sec-rg"
}