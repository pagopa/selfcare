output "internal_private_dns_zone_name" {
  value = azurerm_private_dns_zone.internal_private_dns_zone.name
}

output "privatelink_servicebus_windows_net_id" {
  value = azurerm_private_dns_zone.privatelink_servicebus_windows_net.id
}

output "privatelink_servicebus_windows_net_name" {
  value = azurerm_private_dns_zone.privatelink_servicebus_windows_net.name
}

output "privatelink_documents_azure_com_id" {
  value = azurerm_private_dns_zone.privatelink_documents_azure_com.id
}

output "privatelink_mongo_cosmos_azure_com_id" {
  value = azurerm_private_dns_zone.privatelink_mongo_cosmos_azure_com.id
}

output "privatelink_blob_core_windows_net_id" {
  value = azurerm_private_dns_zone.privatelink_blob_core_windows_net.id
}

output "private_azurecontainerapps_io_id" {
  value = azurerm_private_dns_zone.private_azurecontainerapps_io.id
}
