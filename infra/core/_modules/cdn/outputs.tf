output "fqdn" {
  value = module.checkout_cdn.fqdn
}

output "storage_primary_web_host" {
  value = module.checkout_cdn.storage_primary_web_host
}

output "checkout_fe_rg_name" {
  value = azurerm_resource_group.checkout_fe_rg.name
}
