module "mongodb_onboarding" {
  source = "../_modules/mongodb"

  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name

  databases = {
    "selcOnboarding" = {
      throughput = 1000
      collections = {
        "onboardings" = {
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
          indexes = [
            { keys = ["_id"], unique = true },
            { keys = ["createdAt"], unique = false }
          ]
        }
      }
    }
  }
}
