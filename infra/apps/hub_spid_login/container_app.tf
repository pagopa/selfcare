module "container_app_hub_spid_login" {
  source = "github.com/pagopa/selfcare-commons//infra/terraform-modules/container_app_microservice?ref=v1.1.0"

  is_pnpg = var.is_pnpg

  env_short                      = var.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = var.container_app
  container_app_name             = "hub-spid-login"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "hub-spid-login-ms"
  image_tag                      = var.image_tag
  app_settings                   = var.app_settings
  secrets_names                  = var.secrets_names
  workload_profile_name          = var.workload_profile_name

  user_assigned_identity_id           = data.azurerm_user_assigned_identity.cae_identity.id
  user_assigned_identity_principal_id = data.azurerm_user_assigned_identity.cae_identity.principal_id

  probes = [
    {
      httpGet = {
        path   = "/info"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      type                = "Liveness"
      failureThreshold    = 5
      initialDelaySeconds = 1
    },
    {
      httpGet = {
        path   = "/info"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      type                = "Readiness"
      failureThreshold    = 3
      initialDelaySeconds = 3
    },
    {
      httpGet = {
        path   = "/info"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      failureThreshold    = 30
      type                = "Startup"
      initialDelaySeconds = 30
    }
  ]

  tags = var.tags
}

data "azurerm_user_assigned_identity" "cae_identity" {
  name                = "${local.container_app_environment_name}-managed_identity"
  resource_group_name = local.ca_resource_group_name
}