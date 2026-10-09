data "azurerm_resource_group" "resource_group_app" {
  name = local.resource_group_name
}

data "azurerm_key_vault" "key_vault" {
  resource_group_name = local.key_vault_resource_group_name
  name                = local.key_vault_name
}

data "azurerm_container_app_environment" "container_app_environment" {
  resource_group_name = data.azurerm_resource_group.resource_group_app.name
  name                = var.container_app_environment_name
}

data "azurerm_user_assigned_identity" "cae_identity" {
  count               = var.environment_identity_enabled ? 1 : 0
  name                = "${var.container_app_environment_name}-managed_identity"
  resource_group_name = var.resource_group_name
}