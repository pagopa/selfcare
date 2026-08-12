###############################################################################
# GLOBAL VARIABLES
###############################################################################
# Unified (multitenant) product stack. It replaces `dev-ar` and `dev-pnpg`:
# one container app, one Cosmos database, serving both tenants.
#
# `product` is deployed in the AR container app environment because that is the
# environment the surviving container app already lives in. See README.md — this
# stack adopts existing resources via `terraform import`, it does not create them.
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

###############################################################################
# COSMOS DB
###############################################################################
# The product catalogue is deliberately NOT tenant-discriminated (see EPIC
# sub-task 6): products are global platform configuration shared by every tenant.
# Consolidation therefore means reconciling two catalogues into one, not merging
# two tenant-partitioned datasets — the indexes below stay unchanged.
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
      # Both legacy stacks already resolved this to the same literal container
      # (`sc-d-documents-blob`): contract templates are shared platform assets,
      # not per-tenant data, so there is nothing to split here.
      name  = "BLOB_STORAGE_CONTAINER_CONTRACT_TEMPLATE"
      value = "sc-${module.local.config.env_short}-documents-blob"
    }
  ]

  secrets_names_product_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "SELFCARE_DATA_ENCRIPTION_KEY"          = "selfcare-data-encryption-key"
    "SELFCARE_DATA_ENCRIPTION_IV"           = "selfcare-data-encryption-iv"
    "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
    # Must verify BOTH issuers once PNPG traffic arrives: `auth` for AR and
    # `hub-spid-login` for PNPG. Each legacy stack held only its own key.
    "JWT_PUBLIC_KEY"                                   = "jwt-public-key"
    "BLOB_STORAGE_CONNECTION_STRING_CONTRACT_TEMPLATE" = "documents-storage-connection-string"
  }
}

module "container_app_product_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-product-ms"
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
