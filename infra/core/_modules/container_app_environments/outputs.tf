output "container_app_environment" {
  value = {
    id                  = azurerm_container_app_environment.cae.id
    name                = azurerm_container_app_environment.cae.name
    resource_group_name = azurerm_container_app_environment.cae.resource_group_name
  }
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
