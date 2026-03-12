module "cosmosdb" {
  source = "../_modules/cosmosdb"

  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name

  database_name = "selcOnboarding"

  collections = [
    {
      name      = "onboardings"
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
    {
      name      = "tokens"
      shard_key = "_id"
      indexes = [
        { keys = ["_id"], unique = true },
        { keys = ["createdAt"], unique = false }
      ]
    }
  ]
}

resource "random_password" "encryption_key" {
  length  = 32
  special = false

  keepers = {
    version = 1
  }
}

resource "random_password" "encryption_iv" {
  length  = 12
  special = false

  keepers = {
    version = 1
  }
}

resource "azurerm_key_vault_secret" "encryption_iv_secret" {
  name         = "onboarding-data-encryption-iv"
  value        = random_password.encryption_iv.result
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "encryption_key_secret" {
  name         = "onboarding-data-encryption-key"
  value        = random_password.encryption_key.result
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.key_vault.id
}
