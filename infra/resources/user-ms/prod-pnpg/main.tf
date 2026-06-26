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
# DATA SOURCES
###############################################################################
data "azurerm_storage_account" "product_storage" {
  name                = "selc${module.local.config.env_short}${module.local.config.location_short}${module.local.config.domain}checkoutsa"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-checkout-fe-rg"
}

data "azurerm_user_assigned_identity" "product_storage_blob_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-product-storage-blob-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

###############################################################################
# COSMOS DB
###############################################################################


module "cosmosdb" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcUser"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_user_institutions" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "userInstitutions"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    {
      keys   = ["_id"]
      unique = true
    },
    {
      keys   = ["institutionId"]
      unique = false
    },
    {
      keys   = ["userId", "institutionId"]
      unique = false
    }
  ]
}

module "collection_user_info" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "userInfo"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true }
  ]
}

###############################################################################
# Container App
###############################################################################

locals {
  app_settings_user_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar -Djava.net.preferIPv4Stack=true -Dnetworkaddress.cache.ttl=30 -Dnetworkaddress.cache.negative.ttl=1"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "user-ms",
    },
    {
      name  = "EVENT_HUB_BASE_PATH"
      value = "https://selc-d-eventhub-ns.servicebus.windows.net/sc-users"
    },
    {
      name  = "EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC"
      value = "string"
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-p-product"
    },
    {
      name  = "USER_REGISTRY_URL"
      value = "https://api.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "USER_MS_RETRY_MIN_BACKOFF"
      value = 5
    },
    {
      name  = "USER_MS_RETRY_MAX_BACKOFF"
      value = 60
    },
    {
      name  = "USER_MS_RETRY"
      value = 3
    },
    {
      name  = "SELFCARE_URL"
      value = "https://imprese.notifichedigitali.it"
    },
    {
      name  = "ONBOARDING_URL"
      value = "http://selc-p-pnpg-onboarding-ms-ca"
    },
    {
      name  = "BLOB-STORAGE-PRODUCT-ACCOUNT-NAME"
      value = data.azurerm_storage_account.product_storage.name
    },
    {
      name  = "BLOB-STORAGE-PRODUCT-MANAGED-IDENTITY-CLIENT-ID"
      value = data.azurerm_user_assigned_identity.product_storage_blob_identity.client_id
    },
    {
      name  = "BLOB-STORAGE-TEMPLATES-ACCOUNT-NAME"
      value = data.azurerm_storage_account.product_storage.name
    },
    {
      name  = "BLOB-STORAGE-TEMPLATES-MANAGED-IDENTITY-CLIENT-ID"
      value = data.azurerm_user_assigned_identity.product_storage_blob_identity.client_id
    }
  ]

  secrets_names_user_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
    "JWT-PUBLIC-KEY"                          = "jwt-public-key"
    "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
    "USER-REGISTRY-API-KEY"                   = "user-registry-api-key"
    "AWS-SES-ACCESS-KEY-ID"                   = "aws-ses-access-key-id"
    "AWS-SES-SECRET-ACCESS-KEY"               = "aws-ses-secret-access-key"
  }
}

module "container_app_user_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-${module.local.config.domain}-user-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-user-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_user_ms
  secrets_names                  = local.secrets_names_user_ms
  workload_profile_name          = null
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
  additional_user_assigned_identity_ids = [
    data.azurerm_user_assigned_identity.product_storage_blob_identity.id
  ]
}
