output "storage_account" {
  value = {
    id                        = module.storage_account.id
    name                      = module.storage_account.name
    resource_group_name       = module.storage_account.resource_group_name
    primary_web_host          = module.storage_account.primary_web_host
    primary_connection_string = module.storage_account.primary_connection_string
  }
  sensitive = true
}

output "storage_container_name" {
  value = azurerm_storage_container.selc_user_attachments_blob.name
}

output "kv_secret_name" {
  value       = azurerm_key_vault_secret.selc_user_attachments_storage_connection_string.name
  description = "Name of the Key Vault secret exposing the primary connection string of the user-attachments storage account."
}

output "defender_enabled" {
  value       = var.defender_enabled
  description = "Whether Defender for Storage is enabled on this account."
}

