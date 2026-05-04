terraform {
  required_version = ">= 1.9.0"

  backend "azurerm" {}
}

provider "azurerm" {
  features {}
}

module "container_app_dashboard_backend" {
  source = "github.com/pagopa/selfcare-commons//infra/terraform-modules/container_app_microservice?ref=v1.1.0"

  is_pnpg = var.is_pnpg

  env_short                      = var.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = var.container_app
  container_app_name             = "dashboard-backend"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-dashboard-backend"
  image_tag                      = var.image_tag
  app_settings                   = var.app_settings
  secrets_names                  = var.secrets_names
  workload_profile_name          = var.workload_profile_name

  user_assigned_identity_id           = data.azurerm_user_assigned_identity.cae_identity.id
  user_assigned_identity_principal_id = data.azurerm_user_assigned_identity.cae_identity.principal_id

  tags = var.tags
}

data "azurerm_user_assigned_identity" "cae_identity" {
  name                = "${local.container_app_environment_name}-managed_identity"
  resource_group_name = local.ca_resource_group_name
}