locals {
  pnpg_suffix = var.is_pnpg == true ? "-pnpg" : ""
  mongo_pnpg_suffix =var.is_pnpg == true ? "-weu-pnpg" : ""
  project     = "selc-${var.env_short}"

  container_app_environment_name = "${local.project}${local.pnpg_suffix}-${var.cae_name}"
  ca_resource_group_name         = "${local.project}-container-app${var.suffix_increment}-rg"

  mongo_db = {
    mongodb_rg_name               = "selc-${var.env_short}${local.mongo_pnpg_suffix}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "selc-${var.env_short}${local.mongo_pnpg_suffix}-cosmosdb-mongodb-account"
  }
}