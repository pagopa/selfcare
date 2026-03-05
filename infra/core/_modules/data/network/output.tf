output "vnet_core" {
  value = data.azurerm_virtual_network.vnet_core
}

output "privatelink_mongo_cosmos_azure_com" {
  value = data.azurerm_private_dns_zone.privatelink_mongo_cosmos_azure_com
}

output "privatelink_redis_cache_windows_net_vnet" {
  value = var.redis_private_endpoint_enabled ? data.azurerm_private_dns_zone.privatelink_redis_cache_windows_net_vnet[0] : null
}

output "private_endpoint_subnet" {
  value = data.azurerm_subnet.private_endpoint_subnet
}

output "privatelink_blob_core_windows_net" {
  value = data.azurerm_private_dns_zone.privatelink_blob_core_windows_net
}
