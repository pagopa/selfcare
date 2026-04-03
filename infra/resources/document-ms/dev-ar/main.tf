###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-ar"
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
    { keys = ["attachmentName"], unique = false }
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
    "JWT-PUBLIC-KEY"                          = "jwt-public-key"
    "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
    "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "documents-storage-connection-string"
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
