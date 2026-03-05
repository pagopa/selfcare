output "selfcare_public_dns_zone_name" {
  value = length(azurerm_dns_zone.selfcare_public) > 0 ? azurerm_dns_zone.selfcare_public[0].name : null
}

output "selfcare_public_dns_zone_resource_group_name" {
  value = length(azurerm_dns_zone.selfcare_public) > 0 ? azurerm_dns_zone.selfcare_public[0].resource_group_name : null
}

output "dns_a_api_fqdn" {
  value = azurerm_dns_a_record.dns_a_api.fqdn
}

output "dns_a_api_pnpg_fqdn" {
  value = azurerm_dns_a_record.public_api_pnpg.fqdn
}

output "areariservata_public_dns_zone_name" {
  value = length(azurerm_dns_zone.areariservata_public) > 0 ? azurerm_dns_zone.areariservata_public[0].name : null
}
