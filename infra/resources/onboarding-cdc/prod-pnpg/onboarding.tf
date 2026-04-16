###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env             = "prod"
  env_short       = "p"
  domain          = "pnpg"
  external_domain = "it"

  dns_zone_prefix                = "imprese.notifichedigitali"
  api_dns_zone_prefix            = "api-pnpg.selfcare"
  private_dns_name_domain        = "calmmoss-0be48755.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-p-pnpg-cae-cp"
  ca_resource_group_name         = "selc-p-container-app-rg"
  container_app_max_replicas     = 5
  container_app_desired_replicas = "3"
  container_app_cpu              = 1.25
  container_app_memory           = "2.5Gi"
}

###############################################################################
# Onboarding BFF
###############################################################################

locals {
  onboarding_cdc_app_settings = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "onboarding-cdc"
    },
    {
      name  = "ONBOARDING-CDC-MONGODB-WATCH-ENABLED"
      value = "false"
    },
    {
      name  = "ONBOARDING_FUNCTIONS_URL"
      value = "https://selc-${module.local.config.env_short}-pnpg-onboarding-fn.azurewebsites.net"
    },
    {
      name  = "ONBOARDING-CDC-MINUTES-THRESHOLD-FOR-UPDATE-NOTIFICATION"
      value = "5"
    }
  ]

  onboarding_cdc_secrets_names = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
    "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
    "NOTIFICATION-FUNCTIONS-API-KEY"        = "fn-onboarding-primary-key"
  }

}

module "container_app_onboarding_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-onboarding-cdc"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-onboarding-cdc"
  image_tag                      = var.image_tag
  app_settings                   = local.onboarding_cdc_app_settings
  secrets_names                  = local.onboarding_cdc_secrets_names
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}

