output "storage_account_name" {
  value       = module.storage_account.name
  description = "Storage account name hosting the webhook queue."
}

output "queue_endpoint" {
  value       = module.storage_account.primary_queue_endpoint
  description = "Primary queue endpoint of the storage account."
}

output "queue_name" {
  value       = var.queue_name
  description = "Webhook delivery queue name."
}
