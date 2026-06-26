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
  container_app_max_replicas     = 1
  container_app_desired_replicas = "1"
  container_app_cpu              = 1
  container_app_memory           = "2Gi"
}

###############################################################################
# Container App
###############################################################################

data "azurerm_storage_account" "product_storage" {
  name                = "selc${module.local.config.env_short}${module.local.config.location_short}${module.local.config.domain}checkoutsa"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-checkout-fe-rg"
}

data "azurerm_user_assigned_identity" "product_storage_table_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-product-storage-table-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

data "azurerm_user_assigned_identity" "product_storage_blob_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-product-storage-blob-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

locals {
  app_settings_user_cdc = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar -Djava.net.preferIPv4Stack=true -Dnetworkaddress.cache.ttl=30 -Dnetworkaddress.cache.negative.ttl=1"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "user-cdc",
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-u-product"
    },
    {
      name  = "TABLE_ACCOUNT_NAME"
      value = data.azurerm_storage_account.product_storage.name
    },
    {
      name  = "TABLE_MANAGED_IDENTITY_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.product_storage_table_identity.client_id
    },
    {
      name  = "STORAGE_ACCOUNT_NAME"
      value = data.azurerm_storage_account.product_storage.name
    },
    {
      name  = "STORAGE_MANAGED_IDENTITY_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.product_storage_blob_identity.client_id
    }
  ]

  secrets_names_user_cdc = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
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
  workload_profile_name          = null
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
  additional_user_assigned_identity_ids = [
    data.azurerm_user_assigned_identity.product_storage_table_identity.id,
    data.azurerm_user_assigned_identity.product_storage_blob_identity.id
  ]
}
