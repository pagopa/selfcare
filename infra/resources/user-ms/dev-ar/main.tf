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
}

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
      name  = "USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "EVENT_HUB_BASE_PATH"
      value = "https://selc-d-eventhub-ns.servicebus.windows.net/"
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
  container_app_name             = "selc-${module.local.config.env_short}-user-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-user-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_user_ms
  secrets_names                  = local.secrets_names_user_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}
