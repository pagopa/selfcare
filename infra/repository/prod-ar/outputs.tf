output "repository" {
  value = module.repository
}

output "github_onboarding" {
  value = module.github_onboarding
}

output "subscription_id" {
  value = data.azurerm_subscription.current.subscription_id
}
