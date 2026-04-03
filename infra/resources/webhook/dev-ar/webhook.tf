###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-ar"
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
    { keys = ["productId"], unique = true },
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
    { keys = ["webhookId"], unique = false }
  ]
}

###############################################################################
# Container App
###############################################################################

locals {

  webhook_container_app_name = "${module.local.config.project}-webhook-ms"

  app_settings_webhook_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "webhook-ms"
    },
    {
      name  = "MONGODB_DATABASE_NAME"
      value = "selcWebhook"
    }
  ]

  secrets_names_webhook_ms = {
    "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
    "JWT_PUBLIC_KEY"                        = "jwt-public-key"
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
# APIM
###############################################################################

module "apim_api" {
  source              = "../../_modules/apim_api"
  apim_name           = module.local.config.apim_name
  apim_rg             = module.local.config.apim_rg
  api_name            = "selc-${module.local.config.env_short}-api-webhook"
  display_name        = "Webhook API"
  base_path           = "external/webhook"
  private_dns_name    = "${local.webhook_container_app_name}-ca.${module.local.config.private_dns_name_domain}"
  dns_zone_prefix     = module.local.config.dns_zone_prefix
  api_dns_zone_prefix = module.local.config.api_dns_zone_prefix
  openapi_path        = "../../../../apps/webhook/src/main/docs/openapi.json"

  api_operation_policies = []
}

# <policies>
#           -     <inbound>
#           -             <cors allow-credentials="true">
#           -                     <allowed-origins>
#           -                             <origin>https://dev.selfcare.pagopa.it</origin>
#           -                             <origin>https://api.dev.selfcare.pagopa.it</origin>
#           -                             <origin>http://localhost:3000</origin>
#           -                     </allowed-origins>
#           -                     <allowed-methods>
#           -                             <method>GET</method>
#           -                             <method>POST</method>
#           -                             <method>PUT</method>
#           -                             <method>HEAD</method>
#           -                             <method>DELETE</method>
#           -                             <method>OPTIONS</method>
#           -                     </allowed-methods>
#           -                     <allowed-headers>
#           -                             <header>*</header>
#           -                     </allowed-headers>
#           -             </cors>
#           -             <base />
#           -             <trace source="WEBHOOK" severity="information">
#           -                     <message>WEBHOOK ECHO</message>
#           -                     <metadata name="body" value="@(context.Request.Body.As<string>(preserveContent: true))" />
#           -             </trace>
#           -             <return-response>
#           -                     <set-status code="200" reason="OK" />
#           -                     <set-body>{"status": "received"}</set-body>
#           -             </return-response>
#           -     </inbound>
#           -     <backend>
#           -             <base />
#           -     </backend>
#           -     <outbound>
#           -             <base />
#           -     </outbound>
#           -     <on-error>
#           -             <base />
#           -     </on-error>
#           + <policies>
