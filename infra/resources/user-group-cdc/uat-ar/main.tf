###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "uat"
  env_short = "u"
  domain    = "ar"

  dns_zone_prefix                = "uat.selfcare"
  api_dns_zone_prefix            = "api.uat.selfcare"
  private_dns_name_domain        = "mangopond-2a5d4d65.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-u-cae-002"
  ca_resource_group_name         = "selc-u-container-app-002-rg"
  container_app_max_replicas     = 1
  container_app_desired_replicas = "1"
  container_app_cpu              = 1
  container_app_memory           = "2Gi"
}

###############################################################################
# Container App
###############################################################################

locals {
  app_settings_user_group_cdc = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "user-group-cdc",
    },
    {
      name  = "EVENT_HUB_BASE_PATH"
      value = "https://selc-u-eventhub-ns.servicebus.windows.net/"
    },
    {
      name  = "EVENT_HUB_SC_USERGROUPS_TOPIC"
      value = "sc-usergroups"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "USER_GROUP_CDC_SEND_EVENTS_WATCH_ENABLED"
      value = "true"
    }
  ]

  secrets_names_user_group_cdc = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"      = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"                  = "mongodb-connection-string"
    "STORAGE_CONNECTION_STRING"                  = "blob-storage-product-connection-string"
    "EVENTHUB-SC-USER-GROUPS-SELFCARE-WO-KEY-LC" = "eventhub-sc-usergroups-selfcare-wo-key-lc"
  }
}

module "container_app_user_group_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-user-group-cdc"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-user-group-cdc"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_user_group_cdc
  secrets_names                  = local.secrets_names_user_group_cdc
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}
