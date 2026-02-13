# Core VNet
output "rg_vnet_name" {
  value = azurerm_resource_group.rg_vnet.name
}

output "rg_vnet_location" {
  value = azurerm_resource_group.rg_vnet.location
}

output "vnet_id" {
  value = module.vnet.id
}

output "vnet_name" {
  value = module.vnet.name
}

output "private_endpoints_subnet_id" {
  value = module.private_endpoints_subnet.id
}

# Pair VNet
output "rg_pair_vnet_name" {
  value = azurerm_resource_group.rg_pair_vnet.name
}

output "vnet_pair_id" {
  value = module.vnet_pair.id
}

output "vnet_pair_name" {
  value = module.vnet_pair.name
}

# AKS VNet
output "rg_vnet_aks_name" {
  value = azurerm_resource_group.rg_vnet_aks.name
}

output "rg_vnet_aks_location" {
  value = azurerm_resource_group.rg_vnet_aks.location
}

output "vnet_aks_platform_id" {
  value = module.vnet_aks_platform.id
}

output "vnet_aks_platform_name" {
  value = module.vnet_aks_platform.name
}

output "outbound_ip_aks_platform_id" {
  value = azurerm_public_ip.outbound_ip_aks_platform.id
}

# App Gateway Public IP
output "appgateway_public_ip_id" {
  value = azurerm_public_ip.appgateway_public_ip.id
}

output "appgateway_public_ip_address" {
  value = azurerm_public_ip.appgateway_public_ip.ip_address
}
