output "endpoint_hostname" {
  value = module.checkout_cdn.endpoint_hostname
}

output "storage_primary_web_host" {
  value = module.cdn_storage_account.primary_web_host
}

output "checkout_fe_rg_name" {
  value = azurerm_resource_group.checkout_fe_rg.name
}

output "storage_primary_access_key" {
  value     = data.azurerm_storage_account.cdn.primary_access_key
  sensitive = true
}

output "name" {
  value = module.checkout_cdn.name
}

output "rule_set_id" {
  value = module.checkout_cdn.rule_set_id
}

output "principal_id" {
  value = module.checkout_cdn.principal_id
}

output "storage_name" {
  value = module.cdn_storage_account.name
}

output "old_storage_account_name" {
  description = "Name of the old CDN storage account"
  value       = data.azurerm_storage_account.old_cdn_storage_account.name
}

output "old_storage_account_primary_access_key" {
  description = "Primary access key of the old CDN storage account"
  value       = data.azurerm_storage_account.old_cdn_storage_account.primary_access_key
  sensitive   = true
}