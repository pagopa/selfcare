output "mongodb_onboarding" {
  value = module.mongodb_onboarding
}

output "subscription_id" {
  value = data.azurerm_subscription.current.subscription_id
}
