resource "azurerm_cosmosdb_mongo_database" "this" {
  for_each            = var.databases
  name                = each.key
  resource_group_name = var.resource_group_name
  account_name        = var.account_name
  throughput          = each.value.max_throughput == null ? each.value.throughput : null

  dynamic "autoscale_settings" {
    for_each = each.value.max_throughput != null ? [1] : []
    content {
      max_throughput = each.value.max_throughput
    }
  }
}

# Lock
resource "azurerm_management_lock" "database" {
  for_each   = var.databases
  name       = "mongodb-${each.key}-lock"
  scope      = azurerm_cosmosdb_mongo_database.this[each.key].id
  lock_level = "CanNotDelete"
  notes      = "You cannot destory the db."
}

locals {
  collections = merge([
    for db_name, db_config in var.databases : {
      for coll_name, coll_config in db_config.collections :
      "${db_name}.${coll_name}" => {
        db_name             = db_name
        coll_name           = coll_name
        indexes             = coll_config.indexes
        lock_enable         = coll_config.lock_enable
        default_ttl_seconds = coll_config.default_ttl_seconds
        shard_key           = coll_config.shard_key
        throughput          = coll_config.max_throughput == null ? coll_config.throughput : null
        max_throughput      = coll_config.max_throughput
      }
    }
  ]...)
}

resource "azurerm_cosmosdb_mongo_collection" "this" {
  for_each            = local.collections
  name                = each.value.coll_name
  resource_group_name = var.resource_group_name
  account_name        = var.account_name
  database_name       = azurerm_cosmosdb_mongo_database.this[each.value.db_name].name

  default_ttl_seconds = each.value.default_ttl_seconds
  shard_key           = each.value.shard_key
  throughput          = each.value.throughput

  dynamic "autoscale_settings" {
    for_each = each.value.max_throughput != null ? [1] : []
    content {
      max_throughput = each.value.max_throughput
    }
  }

  dynamic "index" {
    for_each = each.value.indexes
    content {
      keys   = index.value.keys
      unique = index.value.unique
    }
  }
}
