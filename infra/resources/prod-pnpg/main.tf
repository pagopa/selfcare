module "mongodb_onboarding" {
  source = "../_modules/mongodb"

  name                = local.mongo_db.mongodb_name
  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name
}
