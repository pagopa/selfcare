data "azurerm_resource_group" "rg_vnet" {
  name = format("%s-vnet-rg", local.project)
}

data "azurerm_resource_group" "rg_monitor" {
  name = local.monitor_rg_name
}

data "azurerm_application_insights" "application_insights" {
  name                = local.monitor_appinsights_name
  resource_group_name = data.azurerm_resource_group.rg_monitor.name
}

data "azurerm_virtual_network" "vnet" {
  name                = format("%s-vnet", local.project)
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
}