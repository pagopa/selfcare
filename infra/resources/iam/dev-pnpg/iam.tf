###############################################################################
# APIM
###############################################################################

module "apim_api" {
  source              = "../../_modules/apim_api"
  apim_name           = local.apim_name
  apim_rg             = local.apim_rg
  api_name            = "selc-${local.env_short}-api-iam"
  display_name        = "IAM API PNPG"
  base_path           = "${local.apim_base_path}"
  private_dns_name    = local.private_dns_name_ms.private_dns_name_ms
  dns_zone_prefix     = local.dns_zone_prefix
  api_dns_zone_prefix = local.api_dns_zone_prefix
  openapi_path        = "../../../../apps/iam/src/main/docs/openapi.json"

  api_operation_policies = []
}

###############################################################################
# CosmosDB
###############################################################################

module "cosmosdb" {
  source = "../../_modules/cosmosdb_database"

  database_name               = local.mongo_db.database_name
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_iam_user" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "userClaims"
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["email"], unique = true }
  ]

  depends_on = [module.cosmosdb]
}

module "collection_iam_roles" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "roles"
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true }
  ]

  depends_on = [module.cosmosdb]
}

###############################################################################
# Container App
###############################################################################

locals {
  app_settings_iam_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "iam-ms"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "IAM_MS_RETRY_MIN_BACKOFF"
      value = 5
    },
    {
      name  = "IAM_MS_RETRY_MAX_BACKOFF"
      value = 60
    },
    {
      name  = "IAM_MS_RETRY"
      value = 3
    },
    {
      name  = "INSTITUTION_API_URL"
      value = "https://selc-d-ms-core-ca.whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
    }
  ]

  secrets_names_iam_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "SELFCARE_DATA_ENCRIPTION_KEY"          = "selfcare-data-encryption-key"
    "SELFCARE_DATA_ENCRIPTION_IV"           = "selfcare-data-encryption-iv"
    "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
    "JWT_PUBLIC_KEY"                        = "jwt-public-key"
  }
}

module "container_app_iam_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.container_app
  container_app_name             = "${local.project}-iam-ms"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-iam-ms"
  image_tag                      = local.image_tag_latest
  app_settings                   = local.app_settings_iam_ms
  secrets_names                  = local.secrets_names_iam_ms

  key_vault_resource_group_name = local.key_vault_resource_group_name
  key_vault_name                = local.key_vault_name

  probes = local.quarkus_health_probes

  tags = local.tags
}
