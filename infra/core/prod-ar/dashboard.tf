resource "azurerm_portal_dashboard" "overview" {
  name                = "${local.prefix}-${local.env_short}-monitoring-overview"
  resource_group_name = module.log_analytics.monitor_rg_name
  location            = module.log_analytics.monitor_rg_location
  tags                = local.tags

  dashboard_properties = templatefile("${path.module}/../_modules/dashboards/overview-dashboard.json.tpl", {
    subscription_id = module.key_vault.subscription_id
    prefix          = "${local.prefix}-${local.env_short}"
  })
}

resource "azurerm_portal_dashboard" "monitoring_onboarding_event" {
  name                = "${local.prefix}-${local.env_short}-monitoring-onboarding-event"
  resource_group_name = module.log_analytics.monitor_rg_name
  location            = module.log_analytics.monitor_rg_location
  tags                = local.tags

  dashboard_properties = templatefile("${path.module}/../_modules/dashboards/onboarding-events-dashboard.json.tpl", {
    subscription_id = module.key_vault.subscription_id
    prefix          = "${local.prefix}-${local.env_short}"
  })
}

resource "azurerm_portal_dashboard" "monitoring_document_ms" {
  name                = "${local.prefix}-${local.env_short}-monitoring-document-ms"
  resource_group_name = module.log_analytics.monitor_rg_name
  location            = module.log_analytics.monitor_rg_location
  tags                = local.tags

  dashboard_properties = templatefile("${path.module}/../_modules/dashboards/document-ms-dashboard.json.tpl", {
    subscription_id = module.key_vault.subscription_id
    prefix          = "${local.prefix}-${local.env_short}"
  })
}
