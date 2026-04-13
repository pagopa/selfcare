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


###############################################################################
# CosmosDB
###############################################################################

module "cosmosdb_document" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcDocument"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_documents" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "documents"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = "selcDocument"

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["onboardingId"], unique = false },
    { keys = ["rootOnboardingId"], unique = false },
    { keys = ["productId"], unique = false },
    { keys = ["type"], unique = false },
    { keys = ["attachmentName"], unique = false },
    { keys = ["createdAt"], unique = false }
  ]
}

###############################################################################
# Container App
###############################################################################

locals {
  app_settings_document_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "document-ms"
    },
    {
      name  = "SIGNATURE_VALIDATION_ENABLED"
      value = "false"
    },
    {
      name  = "PAGOPA_SIGNATURE_SOURCE"
      value = "namirial"
    },
    {
      name  = "STORAGE_CONTAINER_CONTRACT"
      value = "sc-d-documents-blob"
    },
    {
      name  = "NAMIRIAL_BASE_URL"
      value = "https://selc-d-namirial-sws-ca.${module.local.config.private_dns_name_domain}"
    }
  ]

  secrets_names_document_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
    "JWT_PUBLIC_KEY"                          = "jwt-public-key"
    "MONGODB_CONNECTION_STRING"               = "mongodb-connection-string"
    "BLOB_STORAGE_CONTRACT_CONNECTION_STRING" = "documents-storage-connection-string"
    "NAMIRIAL_SIGN_SERVICE_IDENTITY_USER"     = "namirial-sign-service-user"
    "NAMIRIAL_SIGN_SERVICE_IDENTITY_PASSWORD" = "namirial-sign-service-psw"
  }
}
module "container_app_document_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-document-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-document-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_document_ms
  secrets_names                  = local.secrets_names_document_ms

  key_vault_resource_group_name = module.local.config.key_vault_resource_group_name
  key_vault_name                = module.local.config.key_vault_name

  probes = module.local.config.quarkus_health_probes

  tags = module.local.config.tags
}
