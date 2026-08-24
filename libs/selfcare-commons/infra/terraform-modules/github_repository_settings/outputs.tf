output "tenant_id" {
  value = data.azurerm_client_config.current.tenant_id
}

output "managed_identity_id_dev_ci" {
  value = data.azurerm_user_assigned_identity.identity_dev_ci.client_id
}

output "managed_identity_id_dev_cd" {
  value = data.azurerm_user_assigned_identity.identity_dev_cd.client_id
}

output "managed_identity_id_uat_ci" {
  value = data.azurerm_user_assigned_identity.identity_uat_ci.client_id
}

output "managed_identity_id_uat_cd" {
  value = data.azurerm_user_assigned_identity.identity_uat_cd.client_id
}

output "managed_identity_id_prod_ci" {
  value = data.azurerm_user_assigned_identity.identity_prod_ci.client_id
}

output "managed_identity_id_prod_cd" {
  value = data.azurerm_user_assigned_identity.identity_prod_cd.client_id
}