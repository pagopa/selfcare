locals {
  databases = {
    for db in var.databases : db.name => db
  }

  collections = {
    for coll in var.collections : "${coll.database_name}.${coll.name}" => {
      database_name       = coll.database_name
      name                = coll.name
      indexes             = coll.indexes
      lock_enable         = coll.lock_enable
      default_ttl_seconds = coll.default_ttl_seconds
      shard_key           = coll.shard_key
      throughput          = coll.max_throughput == null ? coll.throughput : null
      max_throughput      = coll.max_throughput
    }
  }
}

resource "azurerm_cosmosdb_mongo_database" "this" {
  for_each            = local.databases
  name                = each.value.name
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
  for_each   = local.databases
  name       = "mongodb-${each.key}-lock"
  scope      = azurerm_cosmosdb_mongo_database.this[each.key].id
  lock_level = "CanNotDelete"
  notes      = "You cannot destory the db."
}

resource "azurerm_cosmosdb_mongo_collection" "this" {
  for_each            = local.collections
  name                = each.value.name
  resource_group_name = var.resource_group_name
  account_name        = var.account_name
  database_name       = azurerm_cosmosdb_mongo_database.this[each.value.database_name].name

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
