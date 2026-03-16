output "cosmosdb" {
  value = module.cosmosdb
}

output "subscription_id" {
  value = data.azurerm_subscription.current.subscription_id
}
