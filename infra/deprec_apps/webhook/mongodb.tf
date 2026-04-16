resource "azurerm_cosmosdb_mongo_database" "selc_webhook" {
  name                = "selcWebhook"
  resource_group_name = local.mongo_db.mongodb_rg_name
  account_name        = local.mongo_db.cosmosdb_account_mongodb_name
}

resource "azurerm_management_lock" "mongodb_selc_webhook" {
  name       = "mongodb-selc-webhook-lock"
  scope      = azurerm_cosmosdb_mongo_database.selc_webhook.id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

module "mongodb_collection_webhooks" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//cosmosdb_mongodb_collection?ref=v7.57.1"

  name                = "webhooks"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.selc_webhook.name

  indexes = [
    {
      keys   = ["_id"]
      unique = true
    },
    {
      keys   = ["productId"]
      unique = true
    },
    {
      keys   = ["products"]
      unique = false
    }
  ]

  lock_enable = true
}

module "mongodb_collection_webhook_notifications" {
  count  = var.is_pnpg ? 0 : 1
  source = "github.com/pagopa/terraform-azurerm-v4.git//cosmosdb_mongodb_collection?ref=v7.57.1"

  name                = "webhookNotifications"
  resource_group_name = local.mongo_db.mongodb_rg_name

  cosmosdb_mongo_account_name  = local.mongo_db.cosmosdb_account_mongodb_name
  cosmosdb_mongo_database_name = azurerm_cosmosdb_mongo_database.selc_webhook.name

  # 30 days
  default_ttl_seconds = 2592000

  indexes = [
    {
      keys   = ["_id"]
      unique = true
    },
    {
      keys   = ["webhookId"]
      unique = false
    }
  ]

  lock_enable = true
}
