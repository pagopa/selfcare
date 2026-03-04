output "subnet" {
  value = {
    id   = azurerm_subnet.container_app_snet.id
    name = azurerm_subnet.container_app_snet.name
  }
}

# output "subnet_pnpg" {
#   value = {
#     id   = azurerm_subnet.pnpg_container_app_snet.id
#     name = azurerm_subnet.pnpg_container_app_snet.name
#   }
# }
