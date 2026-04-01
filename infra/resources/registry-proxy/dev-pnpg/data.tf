data "azurerm_container_app_environment" "cae" {
  name                = module.local.config.container_app_environment_name
  resource_group_name = module.local.config.ca_resource_group_name
}

data "azurerm_container_app" "ca" {
  name                = local.ca_name
  resource_group_name = local.cae_rg_name
}

data "azurerm_storage_account" "existing_logs_storage" {
  name                = local.storage_logs
  resource_group_name = local.storage_logs_rg
}

data "azurerm_search_service" "srch_service" {
  name                = "${module.local.config.prefix}-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-srch"
  resource_group_name = "${module.local.config.prefix}-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-srch-rg"
}