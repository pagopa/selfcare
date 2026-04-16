terraform {
  required_version = ">= 1.9.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "> 4.0.0"
    }
  }

  backend "azurerm" {}
}

provider "azurerm" {
  features {}
}

module "container_app_core" {
  source = "github.com/pagopa/selfcare-commons//infra/terraform-modules/container_app_microservice?ref=v1.1.2"

  is_pnpg = var.is_pnpg

  env_short                      = var.env_short
  container_app                  = var.container_app
  resource_group_name            = local.ca_resource_group_name
  container_app_name             = "ms-core"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-institution-ms"
  image_tag                      = var.image_tag
  app_settings                   = var.app_settings
  secrets_names                  = var.secrets_names
  workload_profile_name          = var.workload_profile_name

  user_assigned_identity_id           = data.azurerm_user_assigned_identity.cae_identity.id
  user_assigned_identity_principal_id = data.azurerm_user_assigned_identity.cae_identity.principal_id

  dapr_settings = []

  probes = [
#     {
#       httpGet = {
#         path   = "/actuator/health/liveness"
#         port   = 8080
#         scheme = "HTTP"
#       }
#       timeoutSeconds      = 5
#       type                = "Liveness"
#       failureThreshold    = 3
#       initialDelaySeconds = 1
#     },
#     {
#       httpGet = {
#         path   = "/actuator/health/readiness"
#         port   = 8080
#         scheme = "HTTP"
#       }
#       timeoutSeconds      = 5
#       type                = "Readiness"
#       failureThreshold    = 30
#       initialDelaySeconds = 3
#     },
#     {
#       httpGet = {
#         path   = "/actuator/health/startup"
#         port   = 8080
#         scheme = "HTTP"
#       }
#       timeoutSeconds      = 15
#       failureThreshold    = 15
#       type                = "Startup"
#       initialDelaySeconds = 15
#     }
  ]

  tags = var.tags
}

data "azurerm_user_assigned_identity" "cae_identity" {
  name                = "${local.container_app_environment_name}-managed_identity"
  resource_group_name = local.ca_resource_group_name
}
