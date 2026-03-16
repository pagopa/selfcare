output "container_app_resource_group_name" {
  value = azurerm_container_app.container_app.location
}

output "container_app_environment_name" {
  value = data.azurerm_container_app_environment.container_app_environment.name
}

output "container_app_name" {
  value = azurerm_container_app.container_app.name
}
