locals {
  prefix            = "selc"
  domain            = "pnpg"
  location_short    = "weu"
  pnpg_suffix_mongo = var.is_pnpg == true ? "-${local.location_short}-${local.domain}" : ""
  pnpg_suffix       = var.is_pnpg == true ? "-pnpg" : ""

  container_app_environment_name = "${local.prefix}-${var.env_short}${local.pnpg_suffix}-${var.cae_name}"
  ca_resource_group_name         = "${local.prefix}-${var.env_short}-container-app${var.suffix_increment}-rg"

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${var.env_short}${local.pnpg_suffix_mongo}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${var.env_short}${local.pnpg_suffix_mongo}-cosmosdb-mongodb-account"
  }
}