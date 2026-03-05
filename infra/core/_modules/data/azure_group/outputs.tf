output "adgroup_admin_object_id" {
  value = data.azuread_group.adgroup_admin.object_id
}

output "adgroup_developers_object_id" {
  value = data.azuread_group.adgroup_developers.object_id
}

output "adgroup_externals_object_id" {
  value = data.azuread_group.adgroup_externals.object_id
}

output "adgroup_security_object_id" {
  value = data.azuread_group.adgroup_security.object_id
}

output "subscription_id" {
  value = data.azurerm_subscription.current.subscription_id
}

output "subscription_name" {
  value = data.azurerm_subscription.current.display_name
}