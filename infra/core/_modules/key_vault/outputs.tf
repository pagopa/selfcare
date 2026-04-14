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

# Secrets query outputs
output "secrets_selfcare_status_dev" {
  value     = var.env_short == "d" ? module.secrets_selfcare_status_dev[0].values : {}
  sensitive = true
}

output "secrets_selfcare_status_uat" {
  value     = var.env_short == "u" ? module.secrets_selfcare_status_uat[0].values : {}
  sensitive = true
}

output "adgroup_admin_id" {
  value = data.azurerm_key_vault_secret.adgroup_admin.value
}

output "adgroup_developers_id" {
  value = data.azurerm_key_vault_secret.adgroup_developers.value
}

output "adgroup_externals_id" {
  value = data.azurerm_key_vault_secret.adgroup_externals.value
}

output "adgroup_security_id" {
  value = data.azurerm_key_vault_secret.adgroup_security.value
}

output "iac_principal_object_id" {
  value = data.azurerm_key_vault_secret.iac_principal.value
}

output "app_projects_principal_object_id" {
  value = data.azurerm_key_vault_secret.app_projects_principal.value
}

output "vpn_app_client_id" {
  value = data.azurerm_key_vault_secret.vpn_app.value
}
