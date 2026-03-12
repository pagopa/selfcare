output "cosmosdb" {
  value = module.cosmosdb
}

output "storage_documents" {
  value     = module.storage_documents
  sensitive = true
}

output "subscription_id" {
  value = data.azurerm_subscription.current.subscription_id
}
