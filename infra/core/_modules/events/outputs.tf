output "event_rg_name" {
  value = azurerm_resource_group.event_rg.name
}

output "eventhub_namespace_name" {
  value = module.event_hub.name
}
