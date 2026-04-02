###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-uat-ar"
}


###############################################################################
# CosmosDB
###############################################################################

module "cosmosdb_webhook" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcWebhook"
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_webhooks" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "webhooks"
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
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
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
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

data "azurerm_container_app_environment" "webhook_container_app_environment" {
  name                = local.container_app_environment_name
  resource_group_name = local.ca_resource_group_name
}

locals {
  webhook_apim_name           = "selc-${local.env_short}-apim-v2"
  webhook_apim_rg             = "selc-${local.env_short}-api-v2-rg"
  webhook_api_name            = "selc-${local.env_short}-api-webhook"
  webhook_api_display_name    = "Webhook API"
  webhook_api_base_path       = "external/webhook"
  webhook_dns_zone_prefix     = "uat.selfcare"
  webhook_api_dns_zone_prefix = "api.uat.selfcare"
  webhook_container_app_name  = "${local.project}-webhook-ms"
  webhook_private_dns_name    = "${local.webhook_container_app_name}-ca.${data.azurerm_container_app_environment.webhook_container_app_environment.default_domain}"
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

  webhook_ms_probes = [
    {
      httpGet = {
        path   = "q/health/live"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      type                = "Liveness"
      failureThreshold    = 3
      initialDelaySeconds = 1
    },
    {
      httpGet = {
        path   = "q/health/ready"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      type                = "Readiness"
      failureThreshold    = 30
      initialDelaySeconds = 3
    },
    {
      httpGet = {
        path   = "q/health/started"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 15
      type                = "Startup"
      failureThreshold    = 15
      initialDelaySeconds = 15
    }
  ]
}

module "container_app_webhook_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.microservice_container_app
  container_app_name             = local.webhook_container_app_name
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-webhook-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_webhook_ms
  secrets_names                  = local.secrets_names_webhook_ms

  key_vault_resource_group_name = local.key_vault_resource_group_name
  key_vault_name                = local.key_vault_name

  probes = local.webhook_ms_probes

  tags = local.tags
}

###############################################################################
# APIM
###############################################################################

resource "azurerm_api_management_api_version_set" "apim_api_webhook" {
  name                = local.webhook_api_name
  resource_group_name = local.webhook_apim_rg
  api_management_name = local.webhook_apim_name
  display_name        = local.webhook_api_display_name
  versioning_scheme   = "Segment"
}

module "apim_api_webhook_ms" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//api_management_api?ref=v9.4.0"
  name                = local.webhook_api_name
  api_management_name = local.webhook_apim_name
  resource_group_name = local.webhook_apim_rg
  version_set_id      = azurerm_api_management_api_version_set.apim_api_webhook.id

  description  = local.webhook_api_display_name
  display_name = local.webhook_api_display_name
  path         = local.webhook_api_base_path
  protocols    = ["https"]

  service_url = "https://${local.webhook_private_dns_name}"

  content_format = "openapi+json"
  content_value = templatefile("../../../../apps/webhook/src/main/docs/openapi.json", {
    url      = format("%s.%s", local.webhook_api_dns_zone_prefix, "pagopa.it")
    basePath = local.webhook_api_base_path
  })

  subscription_required = true

  xml_content = <<XML
<policies>
    <inbound>
        <cors allow-credentials="true">
            <allowed-origins>
                <origin>https://${local.webhook_dns_zone_prefix}.pagopa.it</origin>
                <origin>https://${local.webhook_api_dns_zone_prefix}.pagopa.it</origin>
                <origin>http://localhost:3000</origin>
            </allowed-origins>
            <allowed-methods>
                <method>GET</method>
                <method>POST</method>
                <method>PUT</method>
                <method>HEAD</method>
                <method>DELETE</method>
                <method>OPTIONS</method>
            </allowed-methods>
            <allowed-headers>
                <header>*</header>
            </allowed-headers>
        </cors>
        <base />
        <trace source="WEBHOOK" severity="information">
          <message>WEBHOOK ECHO</message>
          <metadata name="body" value="@(context.Request.Body.As<string>(preserveContent: true))" />
        </trace>
        <return-response>
            <set-status code="200" reason="OK" />
            <set-body>{"status": "received"}</set-body>
        </return-response>
    </inbound>
    <backend>
        <base />
    </backend>
    <outbound>
        <base />
    </outbound>
    <on-error>
        <base />
    </on-error>
</policies>
XML
}
