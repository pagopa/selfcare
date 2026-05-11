data "azurerm_client_config" "current" {}

data "azurerm_subscription" "current" {}

data "azurerm_key_vault" "key_vault" {
  name                = "${local.project}-kv"
  resource_group_name = "${local.project}-sec-rg"
}

data "azurerm_key_vault" "key_vault_pnpg" {
  name                = "${local.project}-pnpg-kv"
  resource_group_name = "${local.project}-pnpg-sec-rg"
}

data "azurerm_virtual_network" "vnet" {
  name                = "${local.project}-vnet"
  resource_group_name = "${local.project}-vnet-rg"
}

data "azurerm_application_insights" "ai" {
  name                = "${local.project}-appinsights"
  resource_group_name = "${local.project}-monitor-rg"
}

data "azurerm_storage_account" "checkout" {
  name                = replace("${local.project}-${var.location_short}-ar-checkout-st-01", "-", "")
  resource_group_name = "${local.project}-checkout-fe-rg"
}

data "azurerm_storage_account" "checkout_pnpg" {
  name                = replace("${local.project}-${var.location_short}-pnpg-checkout-st-01", "-", "")
  resource_group_name = "${local.project}-${var.location_short}-pnpg-checkout-fe-rg"
}

data "azurerm_key_vault_secret" "apim_publisher_email" {
  name         = "apim-publisher-email"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "jwt_kid" {
  name         = "jwt-kid"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "jwt_certificate_data_pem" {
  name         = "jwt-cert"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "jwt_private_key_pem" {
  name         = "jwt-private-key"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "jwt_certificate_data_pem_pnpg" {
  name         = "jwt-cert"
  key_vault_id = data.azurerm_key_vault.key_vault_pnpg.id
}

data "azurerm_key_vault_secret" "jwt_private_key_pem_pnpg" {
  name         = "jwt-private-key"
  key_vault_id = data.azurerm_key_vault.key_vault_pnpg.id
}

# certificate api.selfcare.pagopa.it
data "azurerm_key_vault_certificate" "app_gw_platform" {
  name         = var.app_gateway_api_certificate_name
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "jwt_kid_pnpg" {
  name         = "jwt-kid"
  key_vault_id = data.azurerm_key_vault.key_vault_pnpg.id
}

data "azurerm_key_vault_secret" "web_storage_url" {
  name         = "web-storage-url"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_api_management" "apim" {
  name                = "${local.project}-apim-v2"
  resource_group_name = local.rg_apim_name
}
