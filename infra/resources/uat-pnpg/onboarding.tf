module "mongodb" {
  source = "../_modules/mongodb"

  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name

  databases = {
    "selcOnboarding" = {
      collections = {
        "onboardings" = {
          shard_key = "_id"
          indexes = [
            { keys = ["_id"], unique = true },
            { keys = ["createdAt"], unique = false },
            { keys = ["origin"], unique = false },
            { keys = ["originId"], unique = false },
            { keys = ["taxCode"], unique = false },
            { keys = ["subunitCode"], unique = false },
            { keys = ["productId"], unique = false },
            { keys = ["status"], unique = false }
          ]
        },
        "tokens" = {
          shard_key = "_id"
          indexes = [
            { keys = ["_id"], unique = true },
            { keys = ["createdAt"], unique = false }
          ]
        }
      }
    }
  }
}
