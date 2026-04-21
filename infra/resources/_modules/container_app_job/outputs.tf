output "container_app_job_name" {
  value = azurerm_container_app_job.container_app_job.name
}

output "container_app_environment_name" {
  value = data.azurerm_container_app_environment.container_app_environment.name
}

output "cae_identity_id" {
  value = data.azurerm_user_assigned_identity.cae_identity.id
}

output "cae_identity_client_id" {
  value = data.azurerm_user_assigned_identity.cae_identity.client_id
}