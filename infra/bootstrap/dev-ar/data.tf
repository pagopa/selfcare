data "azurerm_key_vault" "key_vault" {
  name                = "${local.project}-kv"
  resource_group_name = "${local.project}-sec-rg"
}

data "azurerm_key_vault" "key_vault_pnpg" {
  name                = "${local.project}-pnpg-kv"
  resource_group_name = "${local.project}-pnpg-sec-rg"
}

data "azurerm_key_vault_secret" "github_path_token" {
  name         = "github-path-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "github_repository" "repo" {
  full_name = "pagopa/selfcare"
}

data "azurerm_key_vault_secret" "apim_product_pn_sk" {
  name         = "apim-product-pn-sk"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "apim_internal_sk" {
  name         = "apim-internal-sk"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "apim_support_sk" {
  name         = "apim-support-sk"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "apim_product_pnpg_sk" {
  name         = "apim-product-pnpg-sk"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

