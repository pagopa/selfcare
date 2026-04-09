output "container_app_resource_group_name" {
  value = azurerm_container_app.container_app.location
}

output "container_app_environment_name" {
  value = data.azurerm_container_app_environment.container_app_environment.name
}

output "container_app_name" {
  value = azurerm_container_app.container_app.name
}

output "cae_identity_id" {
  value = data.azurerm_user_assigned_identity.cae_identity.id
}

output "cae_identity_client_id" {
  value = data.azurerm_user_assigned_identity.cae_identity.client_id
}