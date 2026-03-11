locals {
  project = "${var.prefix}-${var.env_short}"
}

resource "azurerm_resource_group" "dashboards" {
  name     = "dashboards"
  location = var.location
  tags     = var.tags
}

resource "azurerm_resource_group" "monitor_rg" {
  name     = "${local.project}-monitor-rg"
  location = var.location
  tags     = var.tags
}

resource "azurerm_log_analytics_workspace" "log_analytics_workspace" {
  name                = "${local.project}-law"
  location            = azurerm_resource_group.monitor_rg.location
  resource_group_name = azurerm_resource_group.monitor_rg.name
  sku                 = var.law_sku
  retention_in_days   = var.law_retention_in_days
  daily_quota_gb      = var.law_daily_quota_gb
  tags                = var.tags

  lifecycle {
    ignore_changes = [sku]
  }
}

resource "azurerm_application_insights" "application_insights" {
  name                = "${local.project}-appinsights"
  location            = azurerm_resource_group.monitor_rg.location
  resource_group_name = azurerm_resource_group.monitor_rg.name
  application_type    = "other"
  workspace_id        = azurerm_log_analytics_workspace.log_analytics_workspace.id
  tags                = var.tags
}

#tfsec:ignore:AZU023
resource "azurerm_key_vault_secret" "application_insights_key" {
  name         = "appinsights-instrumentation-key"
  value        = azurerm_application_insights.application_insights.instrumentation_key
  content_type = "text/plain"
  key_vault_id = var.key_vault_id
}
