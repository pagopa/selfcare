output "log_analytics_workspace_id" {
  value = azurerm_log_analytics_workspace.log_analytics_workspace.id
}

output "application_insights_id" {
  value = azurerm_application_insights.application_insights.id
}

output "application_insights_name" {
  value = azurerm_application_insights.application_insights.name
}

output "application_insights_instrumentation_key" {
  value     = azurerm_application_insights.application_insights.instrumentation_key
  sensitive = true
}

output "monitor_rg_name" {
  value = azurerm_resource_group.monitor_rg.name
}

output "monitor_rg_location" {
  value = azurerm_resource_group.monitor_rg.location
}
