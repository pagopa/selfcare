###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-uat-pnpg"
}

###############################################################################
# Onboarding BFF
###############################################################################

locals {

  onboarding_cdc_container_app = {
    min_replicas = 1
    max_replicas = 1
    scale_rules = [
      {
        custom = {
          metadata = {
            "desiredReplicas" = "1"
            "start"           = "0 8 * * MON-FRI"
            "end"             = "0 19 * * MON-FRI"
            "timezone"        = "Europe/Rome"
          }
          type = "cron"
        }
        name = "cron-scale-rule"
      }
    ]
    cpu    = 1
    memory = "2Gi"
  }

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
      value = "https://selc-u-pnpg-onboarding-fn.azurewebsites.net"
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
  container_app_name             = "selc-${module.local.config.env_short}-pnpg-onboarding-cdc"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-onboarding-cdc"
  image_tag                      = module.local.config.image_tag_latest
  app_settings                   = local.onboarding_cdc_app_settings
  secrets_names                  = local.onboarding_cdc_secrets_names
  workload_profile_name          = null
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}

