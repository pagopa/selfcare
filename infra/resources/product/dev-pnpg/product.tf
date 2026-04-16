###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"
  domain    = "pnpg"

  dns_zone_prefix                = "pnpg.dev.selfcare"
  api_dns_zone_prefix            = "api-pnpg.dev.selfcare"
  private_dns_name_domain        = "blackhill-644148c0.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-pnpg-cae-cp"
  ca_resource_group_name         = "selc-d-container-app-rg"
  container_app_min_replicas     = 0
}

###############################################################################
# COSMOS DB
###############################################################################


module "cosmosdb" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcProduct"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_products" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "products"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["productId", "version"], unique = false }
  ]
}

module "collection_contract_templates" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "contractTemplates"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["productId", "name", "version"], unique = true },
    { keys = ["productId", "name", "version", "createdAt"], unique = false }
  ]
}


###############################################################################
# Container App
###############################################################################

locals {
  app_settings_product_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "product-ms"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "PRODUCT_MS_RETRY_MIN_BACKOFF"
      value = 5
    },
    {
      name  = "PRODUCT_MS_RETRY_MAX_BACKOFF"
      value = 60
    },
    {
      name  = "PRODUCT_MS_RETRY"
      value = 3
    },
    {
      name  = "MONGODB_DATABASE_NAME"
      value = "selcProduct"
    },
    {
      name  = "BLOB_STORAGE_CONTAINER_CONTRACT_TEMPLATE"
      value = "sc-d-documents-blob"
    }
  ]

  secrets_names_product_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"            = "appinsights-connection-string"
    "SELFCARE_DATA_ENCRIPTION_KEY"                     = "selfcare-data-encryption-key"
    "SELFCARE_DATA_ENCRIPTION_IV"                      = "selfcare-data-encryption-iv"
    "MONGODB_CONNECTION_STRING"                        = "mongodb-connection-string"
    "JWT_PUBLIC_KEY"                                   = "jwt-public-key"
    "BLOB_STORAGE_CONNECTION_STRING_CONTRACT_TEMPLATE" = "documents-storage-connection-string"
  }
}

module "container_app_product_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-${module.local.config.domain}-product-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-product-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_product_ms
  secrets_names                  = local.secrets_names_product_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}
