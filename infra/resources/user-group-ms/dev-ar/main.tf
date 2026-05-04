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

  database_name               = "selcUserGroup"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_user_groups" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "UserGroups"
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
      keys   = ["parentInstitutionId"]
      unique = false
    },
    {
      keys   = ["productId"]
      unique = false
    },
    {
      keys   = ["members"]
      unique = false
    },
    {
      keys   = ["name"]
      unique = false
    }
  ]
}

locals {
  app_settings_user_group_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
      value = "OFF"
    },
    {
      name  = "MS_USER_GROUP_LOG_LEVEL"
      value = "DEBUG"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "ms-user-group"
    }
  ]

  secrets_names_user_group_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB_CONNECTION_URI"                = "mongodb-connection-string"
    "JWT_TOKEN_PUBLIC_KEY"                  = "jwt-public-key"
  }
}

module "container_app_user_group_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-user-group"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-user-group-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_user_group_ms
  secrets_names                  = local.secrets_names_user_group_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags
}

