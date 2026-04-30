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
  container_app_max_replicas     = 5
  container_app_desired_replicas = "3"
  container_app_cpu              = 1.25
  container_app_memory           = "2.5Gi"
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
      value = "-javaagent:applicationinsights-agent.jar",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "user-ms",
    },
    {
      name  = "EVENT_HUB_BASE_PATH"
      value = "https://selc-p-eventhub-ns.servicebus.windows.net/"
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
      name  = "USER_MS_EVENTHUB_USERS_ENABLED"
      value = true
    },
    {
      name = "USER_MS_EVENTHUB_SELFCARE_FD_ENABLED"
      value = true
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
    }
  ]

  secrets_names_user_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"              = "appinsights-connection-string"
    "JWT-PUBLIC-KEY"                                     = "jwt-public-key"
    "MONGODB-CONNECTION-STRING"                          = "mongodb-connection-string"
    "USER-REGISTRY-API-KEY"                              = "user-registry-api-key"
    "AWS-SES-ACCESS-KEY-ID"                              = "aws-ses-access-key-id"
    "AWS-SES-SECRET-ACCESS-KEY"                          = "aws-ses-secret-access-key"
    "EVENTHUB-SC-USERS-SELFCARE-WO-CONNECTION-STRING-LC" = "eventhub-sc-users-selfcare-wo-connection-string-lc"
    "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"             = "blob-storage-product-connection-string"
    "BLOB-STORAGE-CONTRACT-CONNECTION-STRING"            = "blob-storage-contract-connection-string"
    "EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC"               = "eventhub-sc-users-selfcare-wo-key-lc"
    "EVENTHUB_SELFCARE_FD_EXTERNAL_KEY_LC"               = "eventhub-selfcare-fd-external-interceptor-wo-key-lc"
  }
}

module "container_app_user_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-product-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-product-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_user_ms
  secrets_names                  = local.secrets_names_user_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}
