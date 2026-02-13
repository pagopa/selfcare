output "nat_rg_name" {
  value = azurerm_resource_group.nat_rg.name
}

output "nat_gateway_id" {
  value = azurerm_nat_gateway.nat_gateway.id
}

output "pip_outbound_id" {
  value = azurerm_public_ip.pip_outbound.id
}

output "pip_outbound_ip_address" {
  value = azurerm_public_ip.pip_outbound.ip_address
}

output "functions_pip_outbound_id" {
  value = azurerm_public_ip.functions_pip_outbound.id
}
