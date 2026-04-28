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
      name  = "USER_CDC_SEND_EVENTS_WATCH_ENABLED"
      value = "true"
    },
    {
      name = "USER_CDC_SEND_EVENTS_FD_WATCH_ENABLED"
      value = "true"
    },
    {
      name = "USER_CDC_ADD_ON_AGGREGATES_WATCH_ENABLED"
      value = "true"
    },
    {
      name  = "EVENT_HUB_BASE_PATH"
      value = "https://selc-u-eventhub-ns.servicebus.windows.net/"
    },
    {
      name  = "EVENT_HUB_SC_USERS_TOPIC"
      value = "sc-users"
    },
    {
      name  = "EVENT_HUB_SELFCARE_FD_TOPIC"
      value = "selfcare-fd"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "FD_SHARED_ACCESS_KEY_NAME"
      value = "external-interceptor-wo"
    },
    {
      name  = "USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-u-product"
    },
    {
      name  = "INTERNAL_API_URL"
      value = "https://api.uat.selfcare.pagopa.it/external/internal/v1"
    }
  ]

  secrets_names_user_cdc = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
    "STORAGE_CONNECTION_STRING"               = "blob-storage-product-connection-string"
    "EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC"    = "eventhub-sc-users-selfcare-wo-key-lc"
    "USER-REGISTRY-API-KEY"                   = "user-registry-api-key"
    "EVENTHUB_SELFCARE_FD_EXTERNAL_KEY_LC"    = "eventhub-selfcare-fd-external-interceptor-wo-key-lc"
    "INTERNAL_API_KEY"                        = "internal-api-key"
  }
}

module "container_app_user_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-user-cdc"
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
