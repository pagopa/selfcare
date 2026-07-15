output "product_storage_table_identity_id" {
  value       = azurerm_user_assigned_identity.product_storage_table_identity.id
  description = "ID of the user-assigned identity with Storage Table Data Contributor role"
}

output "product_storage_blob_identity_id" {
  value       = azurerm_user_assigned_identity.product_storage_blob_identity.id
  description = "ID of the user-assigned identity with Storage Blob Data Contributor role"
}

output "product_storage_blob_identity_client_id" {
  value       = azurerm_user_assigned_identity.product_storage_blob_identity.client_id
  description = "Client ID of the user-assigned identity with Storage Blob Data Contributor role"
}

