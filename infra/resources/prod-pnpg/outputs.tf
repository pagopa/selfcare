output "mongodb" {
  value = module.mongodb
}

output "subscription_id" {
  value = data.azurerm_subscription.current.subscription_id
}
