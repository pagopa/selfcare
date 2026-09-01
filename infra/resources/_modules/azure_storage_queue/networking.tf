# The DX storage account module disables public network access and resolves the
# Storage Queue private endpoint through the "privatelink.queue.core.windows.net"
# private DNS zone, which it looks up as a data source. Unlike the blob, mongo and
# servicebus zones, that zone is not created by the core `dns_private` module, so it
# is owned here.
#
# Creating the zone is not enough: without a virtual network link the Container Apps
# Environment does not resolve the private record and falls back to the storage
# account public IP, which is unreachable because public access is disabled. Every
# queue operation (publish and consume) would then fail.
resource "azurerm_private_dns_zone" "queue" {
  name                = "privatelink.queue.core.windows.net"
  resource_group_name = var.private_dns_zone_resource_group_name
  tags                = var.tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "queue" {
  name                  = data.azurerm_virtual_network.this.name
  resource_group_name   = var.private_dns_zone_resource_group_name
  private_dns_zone_name = azurerm_private_dns_zone.queue.name
  virtual_network_id    = data.azurerm_virtual_network.this.id
  registration_enabled  = false
  tags                  = var.tags
}
