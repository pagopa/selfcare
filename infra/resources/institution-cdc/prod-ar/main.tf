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

data "azurerm_user_assigned_identity" "product_storage_table_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-product-storage-table-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

###############################################################################
# Institution CDC
###############################################################################

locals {
  app_settings_institution_cdc = [
    {
      name  = "AZURE_STORAGE_ACCOUNT_NAME"
      value = data.azurerm_storage_account.product_storage.name
    },
    {
      name  = "AZURE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.product_storage_table_identity.client_id
    }
  ]

  secrets_names_institution_cdc = {
    "MONGODB-CONNECTION-STRING" = "mongodb-connection-string"
  }

}

module "container_app_institution_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-institution-cdc"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-institution-cdc"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_institution_cdc
  secrets_names                  = local.secrets_names_institution_cdc
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
  additional_user_assigned_identity_ids = [
    data.azurerm_user_assigned_identity.product_storage_table_identity.id
  ]
}
