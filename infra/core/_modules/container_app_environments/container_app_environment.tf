resource "azurerm_container_app_environment" "cae" {
  name                = var.cae_name
  location            = var.location
  resource_group_name = var.resource_group_name

  log_analytics_workspace_id = var.enable_log ? data.azurerm_log_analytics_workspace.log_analytics_workspace.id : null

  infrastructure_subnet_id           = var.subnet_id
  zone_redundancy_enabled            = var.subnet_id == null ? null : var.zone_redundant
  internal_load_balancer_enabled     = true
  infrastructure_resource_group_name = var.infrastructure_resource_group_name

  dynamic "workload_profile" {
    for_each = var.workload_profiles
    content {
      name                  = workload_profile.value.name
      workload_profile_type = workload_profile.value.workload_profile_type
      minimum_count         = workload_profile.value.minimum_count
      maximum_count         = workload_profile.value.maximum_count
    }
  }

  tags = var.tags
}

resource "azurerm_management_lock" "lock_cae" {
  lock_level = "CanNotDelete"
  name       = "${var.project}-cae"
  notes      = "This Container App Environment cannot be deleted"
  scope      = azurerm_container_app_environment.cae.id
}


resource "azurerm_user_assigned_identity" "cae_identity" {
  name                = "${var.cae_name}-managed_identity"
  location            = var.location
  resource_group_name = var.resource_group_name

  tags = var.tags
}

resource "azurerm_management_lock" "identity_lock" {
  name       = azurerm_user_assigned_identity.cae_identity.name
  scope      = azurerm_user_assigned_identity.cae_identity.id
  lock_level = "CanNotDelete"

  notes = "Lock for the user-assigned managed identity"
}

resource "azurerm_user_assigned_identity" "cdc_identity" {
  name                = "${var.cae_name}-cdc-managed_identity"
  location            = var.location
  resource_group_name = var.resource_group_name

  tags = var.tags
}

resource "azurerm_management_lock" "cdc_identity_lock" {
  name       = azurerm_user_assigned_identity.cdc_identity.name
  scope      = azurerm_user_assigned_identity.cdc_identity.id
  lock_level = "CanNotDelete"

  notes = "Lock for the CDC managed identity"
}

data "azurerm_storage_account" "cdc_table_storage" {
  name                = var.cdc_table_storage_account_name
  resource_group_name = var.cdc_table_storage_account_resource_group
}

resource "azurerm_role_assignment" "cdc_identity_role_assignment" {
  scope                = data.azurerm_storage_account.cdc_table_storage.id
  role_definition_name = "Storage Table Data Contributor"
  principal_id         = azurerm_user_assigned_identity.cdc_identity.principal_id
}
