###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "prod"
  env_short = "p"
  domain    = "ar"

  dns_zone_prefix                = "selfcare"
  api_dns_zone_prefix            = "api.selfcare"
  private_dns_name_domain        = "lemonpond-bb0b750e.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-p-cae-002"
  ca_resource_group_name         = "selc-p-container-app-002-rg"
  container_app_max_replicas     = 5
  container_app_min_replicas     = 2
  container_app_desired_replicas = "3"
  container_app_cpu              = 1.5
  container_app_memory           = "3Gi"
}

data "azurerm_user_assigned_identity" "cae_identity" {
  name                = "${module.local.config.container_app_environment_name}-managed_identity"
  resource_group_name = module.local.config.ca_resource_group_name
}


###############################################################################
# CosmosDB
###############################################################################

module "cosmosdb_webhook" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcWebhook"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_webhooks" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "webhooks"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = "selcWebhook"

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["productId", "tenantId"], unique = true },
    { keys = ["products"], unique = false }
  ]
}

module "collection_webhook_notifications" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "webhookNotifications"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = "selcWebhook"
  default_ttl_seconds         = 2592000

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["webhookId"], unique = false },
    # Backs the outbox lag query (filter on status + busPublishedAt, sort by createdAt).
    # Cosmos DB for MongoDB rejects a sort that is not fully covered by an index, so this
    # compound index is required and not just an optimisation.
    { keys = ["status", "busPublishedAt", "createdAt"], unique = false },
    # Backs the claim query in findAndLockPendingNotifications (status + processing lock).
    { keys = ["status", "processing", "processingUntil"], unique = false }
  ]
}

module "collection_webhook_notification_attempts" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "webhookNotificationAttempts"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = "selcWebhook"
  # Same retention as the parent notification, otherwise attempts would grow unbounded.
  default_ttl_seconds = 2592000

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    # Backs findByNotificationId, which filters on notificationId and sorts by attemptNumber.
    { keys = ["notificationId", "attemptNumber"], unique = false }
  ]
}

###############################################################################
# Storage Queue
###############################################################################

module "storage_queue" {
  source = "../../_modules/azure_storage_queue"

  environment = {
    prefix          = module.local.config.prefix
    env_short       = module.local.config.env_short
    location        = module.local.config.location
    location_short  = module.local.config.location_short
    app_name        = "webhook"
    instance_number = "01"
  }

  resource_group_name                         = module.local.config.ca_resource_group_name
  private_endpoint_subnet_name                = "${module.local.config.project}-private-endpoints-snet"
  virtual_network_name                        = module.local.vnet_selc_name
  virtual_network_resource_group_name         = module.local.vnet_resource_group_name
  private_dns_zone_resource_group_name        = module.local.vnet_resource_group_name
  container_app_environment_identity_name     = "${module.local.config.container_app_environment_name}-managed_identity"
  log_analytics_workspace_name                = "${module.local.config.project}-law"
  log_analytics_workspace_resource_group_name = "${module.local.config.project}-monitor-rg"
  subscription_id                             = module.local.subscription_id
  location                                    = module.local.config.location
  queue_name                                  = "webhook-notifications"
  tags                                        = module.local.config.tags
}

###############################################################################
# Container App
###############################################################################

locals {

  webhook_container_app_name = "${module.local.config.project}-webhook-ms"

  app_settings_webhook_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar -Xmx800m -XX:MaxDirectMemorySize=256m -XX:MaxMetaspaceSize=256m -Dio.netty.leakDetection.level=advanced"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "webhook-ms"
    },
    {
      name  = "MONGODB_DATABASE_NAME"
      value = "selcWebhook"
    },
    {
      name  = "WEBHOOK_STORAGE_QUEUE_ENABLED"
      value = "true"
    },
    {
      name  = "WEBHOOK_STORAGE_QUEUE_ENDPOINT"
      value = module.storage_queue.queue_endpoint
    },
    {
      name  = "WEBHOOK_STORAGE_QUEUE_NAME"
      value = module.storage_queue.queue_name
    },
    {
      name  = "WEBHOOK_STORAGE_QUEUE_POISON_QUEUE"
      value = module.storage_queue.poison_queue_name
    },
    {
      name  = "AZURE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.cae_identity.client_id
    }
  ]

  secrets_names_webhook_ms = {
    "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
    "JWT_PUBLIC_KEY"                        = "jwt-public-key"
    "WEBHOOK_JWT_PRIVATE_KEY"               = "jwt-private-key-pkcs8"
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "SELFCARE_DATA_ENCRIPTION_KEY"          = "selfcare-data-encryption-key"
    "SELFCARE_DATA_ENCRIPTION_IV"           = "selfcare-data-encryption-iv"
  }


}

module "container_app_webhook_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = local.webhook_container_app_name
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-webhook-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_webhook_ms
  secrets_names                  = local.secrets_names_webhook_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}

###############################################################################
# Synthetic monitoring
###############################################################################

data "azurerm_container_app_environment" "webhook" {
  name                = module.local.config.container_app_environment_name
  resource_group_name = module.local.config.ca_resource_group_name
}

data "azurerm_monitor_action_group" "email" {
  name                = "PagoPA"
  resource_group_name = "${module.local.config.project}-monitor-rg"
}

data "azurerm_monitor_action_group" "slack" {
  name                = "SlackPagoPA"
  resource_group_name = "${module.local.config.project}-monitor-rg"
}

module "webhook_synthetic_monitoring" {
  source = "../../_modules/application_insights_synthetic_monitoring"

  prefix                              = "${module.local.config.project_location}-webhook"
  location                            = module.local.config.location
  resource_group_name                 = module.local.config.ca_resource_group_name
  container_app_environment_id        = data.azurerm_container_app_environment.webhook.id
  user_assigned_identity_id           = data.azurerm_user_assigned_identity.cae_identity.id
  user_assigned_identity_client_id    = data.azurerm_user_assigned_identity.cae_identity.client_id
  user_assigned_identity_principal_id = data.azurerm_user_assigned_identity.cae_identity.principal_id
  storage_account_name                = "${replace(module.local.config.project_location, "-", "")}synthmon"
  storage_account_resource_group_name = "${module.local.config.project}-synthetic-monitoring-rg"
  image_tag                           = var.image_tag
  application_insight_name            = "${module.local.config.project}-appinsights"
  application_insight_rg_name         = "${module.local.config.project}-monitor-rg"
  application_insights_action_group_ids = [
    data.azurerm_monitor_action_group.email.id,
    data.azurerm_monitor_action_group.slack.id
  ]

  diagnostics_url = "https://${local.webhook_container_app_name}-ca.${module.local.config.private_dns_name_domain}/q/health/group/diagnostics"
  tags            = module.local.config.tags
}

###############################################################################
# APIM
###############################################################################

module "apim_api" {
  source                 = "../../_modules/apim_api"
  apim_name              = module.local.config.apim_name
  apim_rg                = module.local.config.apim_rg
  api_name               = "selc-${module.local.config.env_short}-api-webhook"
  display_name           = "Webhook API"
  base_path              = "external/webhook"
  private_dns_name       = "${local.webhook_container_app_name}-ca.${module.local.config.private_dns_name_domain}"
  dns_zone_prefix        = module.local.config.dns_zone_prefix
  api_dns_zone_prefix    = module.local.config.api_dns_zone_prefix
  openapi_path           = "../../../../apps/webhook/src/main/docs/openapi.json"
  tenant_ids             = module.local.config.tenant_ids
  tenant_hosts           = module.local.config.tenant_hosts
  api_operation_policies = []
}
