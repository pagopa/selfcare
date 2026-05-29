data "azurerm_client_config" "current" {}

resource "azurerm_portal_dashboard" "overview" {
  name                = "${var.prefix}-${var.env_short}-monitoring-overview"
  resource_group_name = var.monitor_rg_name
  location            = var.monitor_rg_location
  tags                = var.tags

  dashboard_properties = templatefile("${path.module}/overview-dashboard.json.tpl", {
    subscription_id = data.azurerm_client_config.current.subscription_id
    prefix          = "${var.prefix}-${var.env_short}"
  })
}
