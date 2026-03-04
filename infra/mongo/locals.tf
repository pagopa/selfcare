locals {
  prefix         = "selc"
  domain         = "pnpg"
  location_short = "weu"

  pnpg_suffix = var.is_pnpg == true ? "-${local.location_short}-${local.domain}" : ""

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${var.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${var.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-account"
  }
}