resource "azurerm_portal_dashboard" "overview" {
  name                = "${local.prefix}-${local.env_short}-monitoring-overview"
  resource_group_name = data.azurerm_resource_group.monitor_rg.name
  location            = local.location
  tags                = local.tags

  dashboard_properties = templatefile("${path.module}/../_modules/dashboards/overview-dashboard.json.tpl", {
    subscription_id = module.key_vault.subscription_id
    prefix          = "${local.prefix}-${local.env_short}"
  })
}
