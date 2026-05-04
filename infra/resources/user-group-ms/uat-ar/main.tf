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
  container_app_max_replicas     = 2
  container_app_desired_replicas = "1"
  container_app_cpu              = 0.5
  container_app_memory           = "1Gi"
}

###############################################################################
# COSMOS DB
###############################################################################


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

###############################################################################
# Container App
###############################################################################

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
      value = "INFO"
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
  container_app_name             = "${module.local.config.project}-user-group"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-user-group-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_user_group_ms
  secrets_names                  = local.secrets_names_user_group_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags
}
