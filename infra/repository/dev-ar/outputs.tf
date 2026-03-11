output "repository" {
  value = module.repository
}

output "subscription_id" {
  value = data.azurerm_subscription.current.subscription_id
}
