###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"
  domain    = "ar"

  dns_zone_prefix                = "dev.selfcare"
  api_dns_zone_prefix            = "api.dev.selfcare"
  private_dns_name_domain        = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-cae-002"
  ca_resource_group_name         = "selc-d-container-app-002-rg"
  container_app_min_replicas     = 0
  container_app_cpu              = 1
  container_app_memory           = "2Gi"
}

###############################################################################
# Delegation CDC
###############################################################################

locals {
  app_settings_delegation_cdc = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "delegation-cdc",
    },
    {
      name  = "DELEGATION_CDC_SEND_EVENTS_WATCH_ENABLED"
      value = "true"
    },
    {
      name  = "EVENT_HUB_BASE_PATH"
      value = "https://selc-d-eventhub-ns.servicebus.windows.net/"
    },
    {
      name  = "EVENT_HUB_SC_DELEGATIONS_TOPIC"
      value = "sc-delegations"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    }
  ]

  secrets_names_delegation_cdc = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"      = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"                  = "mongodb-connection-string"
    "STORAGE_CONNECTION_STRING"                  = "blob-storage-product-connection-string"
    "EVENTHUB-SC-DELEGATIONS-SELFCARE-WO-KEY-LC" = "eventhub-sc-delegations-selfcare-wo-key-lc"
  }

}

module "container_app_delegation_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-delegation-cdc"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-delegation-cdc"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_delegation_cdc
  secrets_names                  = local.secrets_names_delegation_cdc
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}
