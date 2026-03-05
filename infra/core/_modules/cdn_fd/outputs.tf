output "profile_id" {
  value = module.checkout_cdn.profile_id
}

output "profile_name" {
  value = module.checkout_cdn.profile_name
}

output "endpoint_hostname" {
  value = module.checkout_cdn.hostname
}

output "storage_primary_web_host" {
  value = module.checkout_cdn.storage_primary_web_host
}

output "checkout_fe_rg_name" {
  value = azurerm_resource_group.checkout_fe_rg.name
}

output "storage_primary_access_key" {
  value     = module.checkout_cdn.storage_primary_access_key
  sensitive = true
}

output "storage_name" {
  value = module.checkout_cdn.storage_name
}

# Backward-compatible output: endpoint name follows the same
# "{prefix}-cdn-endpoint" convention used by CDN Classic, so
# downstream modules (assets, one_trust) that derive storage-account
# and profile names via string replacement keep working.
output "name" {
  value = module.checkout_cdn.endpoint_name
}