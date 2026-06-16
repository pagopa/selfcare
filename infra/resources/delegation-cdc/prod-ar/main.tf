###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "prod"
  env_short = "p"
  domain    = "ar"

  dns_zone_prefix                = "selfcare"
  api_dns_zone_prefix            = "api.selfcare"
  private_dns_name_domain        = "lemonpond-bb0b750e.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-p-cae-002"
  ca_resource_group_name         = "selc-p-container-app-002-rg"
  container_app_cpu              = 1
  container_app_memory           = "2Gi"
}

###############################################################################
# DATA SOURCES
###############################################################################
data "azurerm_storage_account" "product_storage" {
  name                = "selc${module.local.config.env_short}${module.local.config.location_short}archeckoutst01"
  resource_group_name = "selc-${module.local.config.env_short}-checkout-fe-rg"
}

data "azurerm_user_assigned_identity" "cae_identity" {
  name                = "${module.local.config.container_app_environment_name}-managed_identity"
  resource_group_name = module.local.config.ca_resource_group_name
}

###############################################################################
# RBAC
###############################################################################
resource "azurerm_role_assignment" "delegation_cdc_table_contributor" {
  scope                = data.azurerm_storage_account.product_storage.id
  role_definition_name = "Storage Table Data Contributor"
  principal_id         = data.azurerm_user_assigned_identity.cae_identity.principal_id
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
      value = "https://selc-p-eventhub-ns.servicebus.windows.net/"
    },
    {
      name  = "EVENT_HUB_SC_DELEGATIONS_TOPIC"
      value = "sc-delegations"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "AZURE_STORAGE_ACCOUNT_NAME"
      value = data.azurerm_storage_account.product_storage.name
    },
    {
      name  = "AZURE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.cae_identity.client_id
    }
  ]

  secrets_names_delegation_cdc = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"      = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"                  = "mongodb-connection-string"
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
