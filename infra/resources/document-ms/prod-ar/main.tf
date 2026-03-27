###############################################################################
# CosmosDB
###############################################################################

module "cosmosdb_document" {
  source = "../../_modules/cosmosdb_database"

  database_name               = local.mongo_db.database_document_name
  resource_group_name         = local.mongo_db.resource_group_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_mongo_account_name
}

module "collection_documents" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "documents"
  resource_group_name         = local.mongo_db.resource_group_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_mongo_account_name
  database_name               = local.mongo_db.database_document_name

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

module "container_app_document_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = local.environment.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.container_app
  container_app_name             = "${local.project}-document-ms"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-document-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_document_ms
  secrets_names                  = local.secrets_names_document_ms

  key_vault_resource_group_name = local.key_vault_resource_group_name
  key_vault_name                = local.key_vault_name

  probes = local.quarkus_health_probes

  tags = local.tags
}
