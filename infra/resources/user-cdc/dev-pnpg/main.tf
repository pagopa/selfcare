###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"
  domain    = "pnpg"

  dns_zone_prefix                = "pnpg.dev.selfcare"
  api_dns_zone_prefix            = "api-pnpg.dev.selfcare"
  private_dns_name_domain        = "blackhill-644148c0.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-pnpg-cae-cp"
  ca_resource_group_name         = "selc-d-container-app-rg"
  container_app_min_replicas     = 0
}

###############################################################################
# Container App
###############################################################################

locals {
  container_app_user_cdc = {
    min_replicas = 0
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


  app_settings_user_cdc = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "user-cdc",
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-d-product"
    }
  ]

  secrets_names_user_cdc = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
    "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
  }
}

module "container_app_user_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-${module.local.config.domain}-user-cdc"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-user-cdc"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_user_cdc
  secrets_names                  = local.secrets_names_user_cdc
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}
