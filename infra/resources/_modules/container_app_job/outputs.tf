output "container_app_job_name" {
  value = azurerm_container_app_job.container_app_job.name
}

output "container_app_job_id" {
  description = "Resource ID of the Container Apps Job."
  value       = azurerm_container_app_job.container_app_job.id
}

output "container_app_environment_name" {
  value = data.azurerm_container_app_environment.container_app_environment.name
}

output "cae_identity_id" {
  value = var.environment_identity_enabled ? data.azurerm_user_assigned_identity.cae_identity[0].id : null
}

output "cae_identity_client_id" {
  value = var.environment_identity_enabled ? data.azurerm_user_assigned_identity.cae_identity[0].client_id : null
}

output "cae_identity_principal_id" {
  value = var.environment_identity_enabled ? data.azurerm_user_assigned_identity.cae_identity[0].principal_id : null
}

output "system_assigned_identity_principal_id" {
  value = azurerm_container_app_job.container_app_job.identity[0].principal_id
}