
data "azurerm_key_vault" "key_vault" {
  name                = "${local.project}-kv"
  resource_group_name = "${local.project}-sec-rg"
}

data "azurerm_key_vault_secret" "web_storage_access_key" {
  name         = "web-storage-access-key"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_resource_group" "checkout_fe_rg" {
  name = "${local.project_full}-checkout-fe-rg"
}