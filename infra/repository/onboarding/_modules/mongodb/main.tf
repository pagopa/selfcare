resource "azurerm_cosmosdb_mongo_database" "this" {
  name                = var.name
  resource_group_name = var.resource_group_name
  account_name        = var.account_name
  throughput          = var.throughput
}

module "collection_onboardings" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//cosmosdb_mongodb_collection?ref=v7.39.0"

  name                = "onboardings"
  resource_group_name = var.resource_group_name

  cosmosdb_mongo_account_name  = var.account_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.this.name

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

module "collection_tokens" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//cosmosdb_mongodb_collection?ref=v7.39.0"

  name                = "tokens"
  resource_group_name = var.resource_group_name

  cosmosdb_mongo_account_name  = var.account_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.this.name

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
