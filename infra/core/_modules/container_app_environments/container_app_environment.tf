resource "azurerm_container_app_environment" "cae" {
  name                = var.cae_name
  location            = var.location
  resource_group_name = var.resource_group_name

  log_analytics_workspace_id = data.azurerm_log_analytics_workspace.log_analytics_workspace.id

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