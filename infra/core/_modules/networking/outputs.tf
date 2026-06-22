output "subnet" {
  value = {
    id   = azurerm_subnet.container_app_snet.id
    name = azurerm_subnet.container_app_snet.name
  }
}

output "network_security_group_id" {
  value       = try(azurerm_network_security_group.subnet_nsg[0].id, null)
  description = "ID of the network security group associated with the Container App Environment subnet, when managed by this module."
}

# output "subnet_pnpg" {
#   value = {
#     id   = azurerm_subnet.pnpg_container_app_snet.id
#     name = azurerm_subnet.pnpg_container_app_snet.name
#   }
# }
