output "name" {
  value       = azurerm_storage_account.this.name
  description = "Synthetic monitoring Storage Account name."
}

output "id" {
  value       = azurerm_storage_account.this.id
  description = "Synthetic monitoring Storage Account ID."
}

output "resource_group_name" {
  value       = var.resource_group_name
  description = "Synthetic monitoring resource group name."
}
