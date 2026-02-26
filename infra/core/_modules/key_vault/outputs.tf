output "key_vault_id" {
  value = module.key_vault.id
}

output "key_vault_name" {
  value = module.key_vault.name
}

output "key_vault_resource_group_name" {
  value = module.key_vault.resource_group_name
}

output "sec_rg_name" {
  value = azurerm_resource_group.sec_rg.name
}

output "sec_rg_location" {
  value = azurerm_resource_group.sec_rg.location
}

output "tenant_id" {
  value = data.azurerm_client_config.current.tenant_id
}

output "subscription_id" {
  value = data.azurerm_subscription.current.subscription_id
}

output "subscription_name" {
  value = data.azurerm_subscription.current.display_name
}

output "adgroup_admin_object_id" {
  value = data.azuread_group.adgroup_admin.object_id
}

output "adgroup_developers_object_id" {
  value = data.azuread_group.adgroup_developers.object_id
}

# Secrets query outputs
output "secrets_selfcare_status_dev" {
  value     = var.env_short == "d" ? module.secrets_selfcare_status_dev[0].values : {}
  sensitive = true
}

output "secrets_selfcare_status_uat" {
  value     = var.env_short == "u" ? module.secrets_selfcare_status_uat[0].values : {}
  sensitive = true
}
