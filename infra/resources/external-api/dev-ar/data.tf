data "azurerm_container_app_environment" "cae" {
  name                = module.local.config.container_app_environment_name
  resource_group_name = module.local.config.ca_resource_group_name
}

# data "azurerm_storage_account" "existing_logs_storage" {
#   name                = local.storage_logs
#   resource_group_name = local.storage_logs_rg
# }

# data "azurerm_search_service" "srch_service" {
#   name                = "${module.local.config.prefix}-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-srch"
#   resource_group_name = "${module.local.config.prefix}-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-srch-rg"
# }

# data "azurerm_resource_group" "rg_vnet" {
#   name = format("%s-vnet-rg", local.project)
# }

data "azurerm_resource_group" "rg_monitor" {
  name = local.monitor_rg_name
}

data "azurerm_application_insights" "application_insights" {
  name                = local.monitor_appinsights_name
  resource_group_name = data.azurerm_resource_group.rg_monitor.name
}

# data "azurerm_virtual_network" "vnet" {
#   name                = format("%s-vnet", local.project)
#   resource_group_name = data.azurerm_resource_group.rg_vnet.name
# }

# data "azurerm_key_vault" "key_vault" {
#   resource_group_name = var.key_vault.resource_group_name
#   name                = var.key_vault.name
# }

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

# data "azurerm_key_vault" "key_vault_pnpg" {
#   resource_group_name = var.key_vault_pnpg.resource_group_name
#   name                = var.key_vault_pnpg.name
# }

# data "azurerm_key_vault_secret" "apim_product_pnpg_sk" {
#   name         = "external-api-key"
#   key_vault_id = data.azurerm_key_vault.key_vault_pnpg.id
# }

data "github_repository" "repo" {
  full_name = "pagopa/selfcare"
}
