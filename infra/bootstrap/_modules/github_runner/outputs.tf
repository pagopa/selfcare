output "subnet_name" {
  value       = module.subnet_runner.name
  description = "Subnet name"
}

output "subnet_id" {
  value       = module.subnet_runner.id
  description = "Subnet id"
}

output "cae_id" {
  value       = module.container_app_environment_runner.id
  description = "Container App Environment id"
}

output "cae_name" {
  value       = module.container_app_environment_runner.name
  description = "Container App Environment name"
}

output "ca_job_id" {
  value       = module.container_app_job.id
  description = "Container App job id"
}

output "ca_job_name" {
  value       = module.container_app_job.name
  description = "Container App job name"
}

output "user_assigned_identity" {
  value = {
    id           = azurerm_user_assigned_identity.cae_identity.id
    name         = azurerm_user_assigned_identity.cae_identity.name
    client_id    = azurerm_user_assigned_identity.cae_identity.client_id
    principal_id = azurerm_user_assigned_identity.cae_identity.principal_id
  }

  description = "Details about the user-assigned managed identity created to manage roles of the Container Apps of Selfcare Environment"
}
