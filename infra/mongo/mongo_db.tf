resource "azurerm_cosmosdb_mongo_database" "selc_onboarding" {
  name                = "selcOnboarding"
  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name
}

module "mongodb_collection_onboardings" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//cosmosdb_mongodb_collection?ref=v7.39.0"

  name                = "onboardings"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.selc_onboarding.name

  indexes = [{
    keys   = ["_id"]
    unique = true
    },
    {
      keys   = ["createdAt"]
      unique = false
    },
    {
      keys   = ["origin"]
      unique = false
    },
    {
      keys   = ["originId"]
      unique = false
    },
    {
      keys   = ["taxCode"]
      unique = false
    },
    {
      keys   = ["subunitCode"]
      unique = false
    },
    {
      keys   = ["productId"]
      unique = false
    },
    {
      keys   = ["status"]
      unique = false
    }
  ]

  lock_enable = true
}

module "mongodb_collection_tokens" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//cosmosdb_mongodb_collection?ref=v7.39.0"

  name                = "tokens"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.selc_onboarding.name

  indexes = [{
    keys   = ["_id"]
    unique = true
    },
    {
      keys   = ["createdAt"]
      unique = false
    }
  ]

  lock_enable = true
}
