###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env             = "uat"
  env_short       = "u"
  domain          = "pnpg"
  external_domain = "it"

  dns_zone_prefix                = "imprese.uat.notifichedigitali"
  api_dns_zone_prefix            = "api-pnpg.uat.selfcare"
  private_dns_name_domain        = "orangeground-0bd2d4dc.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-u-pnpg-cae-001"
  ca_resource_group_name         = "selc-u-container-app-001-rg"
}

locals {
  container_app_user_group_ms = {
    min_replicas = 1
    max_replicas = 2
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
    cpu    = 0.5
    memory = "1Gi"
  }

  app_settings_user_group_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
      value = "OFF"
    },
    {
      name  = "MS_USER_GROUP_LOG_LEVEL"
      value = "INFO"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "ms-user-group"
    }
  ]

  secrets_names_user_group_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB_CONNECTION_URI"                = "mongodb-connection-string"
    "JWT_TOKEN_PUBLIC_KEY"                  = "jwt-public-key"
  }
}

###############################################################################
# Onboarding BFF
###############################################################################

module "container_app_user_group_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-pnpg-user-group"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-user-group-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_user_group_ms
  secrets_names                  = local.secrets_names_user_group_ms
  workload_profile_name          = null
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags
}

