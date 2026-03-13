locals {
  api_name     = "selc-${local.env_short}-api-auth"
  display_name = "Auth API"
  base_path    = "auth"
}

# api_name     = var.is_pnpg ? "selc-${var.env_short}-pnpg-api-auth" : "selc-${var.env_short}-api-auth"
#   display_name = var.is_pnpg ? "PNPG Auth API" : "Auth API"
#   base_path    = var.is_pnpg ? "imprese/auth" : "auth"

module "apim_api_auth" {
  source              = "../_modules/apim_api"
  apim_name           = local.apim_name
  apim_rg             = local.apim_rg
  api_name            = local.api_name
  display_name        = local.display_name
  base_path           = local.base_path
  private_dns_name    = local.private_dns_name_ms.private_dns_name_auth_ms
  dns_zone_prefix     = local.dns_zone_prefix
  api_dns_zone_prefix = local.api_dns_zone_prefix
  openapi_path        = "../../../apps/auth/src/main/docs/openapi.json"
}


module "cosmosdb_auth" {
  source = "../_modules/cosmosdb_database"

  database_name               = local.mongo_db.database_auth_name
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_auth_otp_flows" {
  source = "../_modules/cosmosdb_collection"

  name                        = "otpFlows"
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
  database_name               = local.mongo_db.database_auth_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["uuid"], unique = true },
    { keys = ["userId"], unique = false },
    { keys = ["expiresAt"], unique = false },
    { keys = ["status"], unique = false },
    { keys = ["userId", "createdAt"], unique = false },
    { keys = ["createdAt"], unique = false }
  ]
}