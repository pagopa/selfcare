data "azurerm_user_assigned_identity" "container_app_environment" {
  name                = var.container_app_environment_identity_name
  resource_group_name = var.resource_group_name
}

data "azurerm_log_analytics_workspace" "monitoring" {
  name                = var.log_analytics_workspace_name
  resource_group_name = var.log_analytics_workspace_resource_group_name
}

data "azurerm_public_ip" "outbound" {
  count = var.sku == "Standard" ? 1 : 0

  name                = var.outbound_public_ip_name
  resource_group_name = var.outbound_public_ip_resource_group_name
}

data "azurerm_subnet" "private_endpoints" {
  count = var.sku == "Premium" ? 1 : 0

  name                 = var.private_endpoint_subnet_name
  virtual_network_name = var.virtual_network_name
  resource_group_name  = var.virtual_network_resource_group_name
}
