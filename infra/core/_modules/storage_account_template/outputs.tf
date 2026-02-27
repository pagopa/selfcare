output "storage_account_id" {
  value = module.storage_account.id
}

output "storage_account_name" {
  value = module.storage_account.name
}

output "primary_access_key" {
  value     = module.storage_account.primary_access_key
  sensitive = true
}

output "primary_connection_string" {
  value     = module.storage_account.primary_connection_string
  sensitive = true
}

output "primary_blob_connection_string" {
  value     = module.storage_account.primary_blob_connection_string
  sensitive = true
}

output "container_name" {
  value = azurerm_storage_container.this.name
}

output "subnet_id" {
  value = module.subnet.id
}
