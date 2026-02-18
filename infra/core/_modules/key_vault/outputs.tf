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

output "appgateway_identity_id" {
  value = azurerm_user_assigned_identity.appgateway.id
}

output "appgateway_identity_principal_id" {
  value = azurerm_user_assigned_identity.appgateway.principal_id
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

output "apim_private_ip_addresses" {
  value = data.azurerm_api_management.apim.private_ip_addresses
}

# Certificate outputs for appgateway
output "app_gw_platform_cert_secret_id" {
  value = data.azurerm_key_vault_certificate.app_gw_platform.secret_id
}

output "app_gw_platform_cert_version" {
  value = data.azurerm_key_vault_certificate.app_gw_platform.version
}

output "api_pnpg_cert_secret_id" {
  value = data.azurerm_key_vault_certificate.api_pnpg_selfcare_certificate.secret_id
}

output "api_pnpg_cert_version" {
  value = data.azurerm_key_vault_certificate.api_pnpg_selfcare_certificate.version
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

output "app_gw_platform_certificate_secret_id" {
  value = data.azurerm_key_vault_certificate.app_gw_platform.secret_id
}

output "api_pnpg_selfcare_certificate_secret_id" {
  value = data.azurerm_key_vault_certificate.api_pnpg_selfcare_certificate.secret_id
}

output "apim_publisher_email" {
  value     = data.azurerm_key_vault_secret.apim_publisher_email.value
  sensitive = true
}

output "sec_workspace_id" {
  value     = try(data.azurerm_key_vault_secret.sec_workspace_id[0].value, null)
  sensitive = true
}

output "sec_storage_id" {
  value     = try(data.azurerm_key_vault_secret.sec_storage_id[0].value, null)
  sensitive = true
}

output "hub_docker_user" {
  value     = data.azurerm_key_vault_secret.hub_docker_user.value
  sensitive = true
}

output "hub_docker_pwd" {
  value     = data.azurerm_key_vault_secret.hub_docker_pwd.value
  sensitive = true
}

output "appgateway_tenant_id" {
  value     = azurerm_user_assigned_identity.appgateway.tenant_id
  sensitive = true
}