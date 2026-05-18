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


