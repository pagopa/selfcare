resource "azurerm_container_app_environment_dapr_component" "eventhub_pubsub" {
  name                         = "${var.prefix}-eventhub-pubsub"
  container_app_environment_id = data.azurerm_container_app_environment.cae.id
  component_type               = "pubsub.kafka"
  version                      = "v1"

  metadata {
    name  = "authType"
    value = "password"
  }

  metadata {
    name  = "saslUsername"
    value = "$ConnectionString"
  }

  metadata {
    name  = "saslPassword"
    value = "Endpoint=sb://${var.queue_url}/;SharedAccessKeyName=selc-proxy;SharedAccessKey=${data.azurerm_key_vault_secret.event_hub_consumer_key.value};EntityPath=${var.queue_topic}"
  }

  metadata {
    name  = "consumerGroup"
    value = var.queue_consumer_group
  }

  metadata {
    name  = "brokers"
    value = "${var.queue_url}:${var.queue_port}"
  }

  scopes = [data.azurerm_container_app.ca.dapr[0].app_id]
}

resource "azurerm_container_app_environment_dapr_component" "appinsight_binding" {
  name                         = "appinsights-binding"
  container_app_environment_id = data.azurerm_container_app_environment.cae.id
  component_type               = "bindings.http"
  version                      = "v1"

  metadata {
    name  = "url"
    value = "https://dc.applicationinsights.azure.com/v2/track"
  }

  metadata {
    name  = "maxBatchSize"
    value = "100"
  }

  metadata {
    name  = "maxBatchInterval"
    value = "10s"
  }

  metadata {
    name  = "method"
    value = "POST"
  }

  metadata {
    name = "headers"
    value = jsonencode({
      "Content-Type" = "application/json"
    })
  }

  scopes = [data.azurerm_container_app.ca.dapr[0].app_id]
}

resource "azurerm_resource_group" "redis_rg" {
  name     = "${var.project}-dapr-redis-rg"
  location = var.location
  tags     = var.tags
}

module "redis" {
  count                         = var.redis_enable == true ? 1 : 0
  source                        = "github.com/pagopa/terraform-azurerm-v4.git//redis_cache?ref=v7.26.5"
  name                          = "${var.project}-dapr-redis"
  resource_group_name           = azurerm_resource_group.redis_rg.name
  location                      = azurerm_resource_group.redis_rg.location
  capacity                      = var.redis_capacity
  redis_version                 = var.redis_version
  family                        = var.redis_family
  sku_name                      = var.redis_sku_name
  public_network_access_enabled = !var.redis_private_endpoint_enabled

  private_endpoint = {
    enabled              = var.redis_private_endpoint_enabled
    virtual_network_id   = data.azurerm_virtual_network.vnet.id
    subnet_id            = data.azurerm_subnet.redis_snet.id
    private_dns_zone_ids = var.redis_private_endpoint_enabled ? [data.azurerm_private_dns_zone.privatelink_redis_cache_windows_net[0].id] : []
  }

  // when azure can apply patch?
  patch_schedules = [
    {
      day_of_week    = "Sunday"
      start_hour_utc = 23
    },
    {
      day_of_week    = "Monday"
      start_hour_utc = 23
    },
    {
      day_of_week    = "Tuesday"
      start_hour_utc = 23
    },
    {
      day_of_week    = "Wednesday"
      start_hour_utc = 23
    },
    {
      day_of_week    = "Thursday"
      start_hour_utc = 23
    },
  ]

  tags = var.tags
}

resource "azurerm_container_app_environment_dapr_component" "redis_state" {
  count                        = var.redis_enable == true ? 1 : 0
  name                         = "redis-state"
  container_app_environment_id = data.azurerm_container_app_environment.cae.id
  component_type               = "state.redis"
  version                      = "v1"

  metadata {
    name  = "redisHost"
    value = "${module.redis[0].hostname}:${module.redis[0].ssl_port}"
  }

  metadata {
    name  = "redisPassword"
    value = module.redis[0].primary_access_key
  }

  metadata {
    name  = "enableTLS"
    value = "true"
  }

  scopes = [data.azurerm_container_app.ca.dapr[0].app_id]
}

# Secrets per Dapr Components
resource "azurerm_container_app_environment_dapr_component" "secrets" {
  name                         = "${var.project}-secrets"
  container_app_environment_id = data.azurerm_container_app_environment.cae.id
  component_type               = "secretstores.azure.keyvault"
  version                      = "v1"

  metadata {
    name  = "vaultName"
    value = data.azurerm_key_vault.key_vault.name
  }

  metadata {
    name  = "azureClientId"
    value = data.azurerm_user_assigned_identity.cae_identity.client_id
  }

  scopes = [data.azurerm_container_app.ca.dapr[0].app_id]


  lifecycle {
    prevent_destroy = true
  }
}