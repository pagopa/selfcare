module "mongodb_collection_pec_notifications" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//cosmosdb_mongodb_collection?ref=v8.26.4"

  name                = "PecNotification"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = "selcMsCore"

  indexes = [{
    keys   = ["_id"]
    unique = true
    },
    {
      keys   = ["moduleDayOfTheEpoch","productId"]
      unique = false
    }
  ]

  lock_enable = true
}

module "mongodb_collection_mail_notification" {
  source = "git::https://github.com/pagopa/terraform-azurerm-v3.git//cosmosdb_mongodb_collection?ref=v8.26.4"

  name                = "MailNotification"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = "selcMsCore"

  indexes = [{
    keys   = ["_id"]
    unique = true
    },
    {
      keys   = ["moduleDayOfTheEpoch"]
      unique = false
    },
    {
      keys   = ["institutionId"]
      unique = true
    }
  ]

  lock_enable = true
}
