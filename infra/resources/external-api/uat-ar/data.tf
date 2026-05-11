data "azurerm_container_app_environment" "cae" {
  name                = module.local.config.container_app_environment_name
  resource_group_name = module.local.config.ca_resource_group_name
}

data "azurerm_resource_group" "rg_monitor" {
  name = local.monitor_rg_name
}

data "azurerm_application_insights" "application_insights" {
  name                = local.monitor_appinsights_name
  resource_group_name = data.azurerm_resource_group.rg_monitor.name
}

data "azurerm_key_vault_secret" "apim_product_pn_sk" {
  name         = "apim-product-pn-sk"
  key_vault_id = module.local.key_vault_id
}

data "azurerm_key_vault_secret" "apim_internal_sk" {
  name         = "apim-internal-sk"
  key_vault_id = module.local.key_vault_id
}

data "azurerm_key_vault_secret" "apim_support_sk" {
  name         = "apim-support-sk"
  key_vault_id = module.local.key_vault_id
}

data "azurerm_key_vault_secret" "apim_product_pnpg_sk" {
  name         = "apim-product-pnpg-sk"
  key_vault_id = module.local.key_vault_id
}

data "github_repository" "repo" {
  full_name = "pagopa/selfcare"
}
