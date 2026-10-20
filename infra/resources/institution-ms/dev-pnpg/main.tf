###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"
  domain    = "pnpg"

  dns_zone_prefix                = "pnpg.dev.selfcare"
  api_dns_zone_prefix            = "api-pnpg.dev.selfcare"
  private_dns_name_domain        = "blackhill-644148c0.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-pnpg-cae-cp"
  ca_resource_group_name         = "selc-d-container-app-rg"
  container_app_min_replicas     = 0
}

###############################################################################
# DATA SOURCES
###############################################################################
data "azurerm_storage_account" "product_storage" {
  name                = "selc${module.local.config.env_short}${module.local.config.location_short}pnpgcheckoutst01"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.location_short}-pnpg-checkout-fe-rg"
}

data "azurerm_user_assigned_identity" "product_storage_blob_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-product-storage-blob-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

module "cosmosdb" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcMsCore"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_institution" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "Institution"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["externalId"], unique = true },
    { keys = ["geographicTaxonomies.code"], unique = false },
    { keys = ["onboarding.productId"], unique = false },
    { keys = ["taxCode"], unique = false }
  ]
}

###############################################################################
# Institution MS (PNPG)
###############################################################################
locals {
  image_tag = var.image_tag

  app_settings_institution_ms = [
    {
      name  = "TENANT_SUPPORTED_TENANTS"
      value = "PNPG"
    },
    {
      name  = "TENANT_DEFAULT"
      value = "PNPG"
    },
    {
      name  = "TENANT_MONGODB_ENABLED"
      value = "true"
    },
    {
      name  = "TENANT_STORAGE_ENABLED"
      value = "true"
    },
    {
      name  = "TENANT_STORAGE_MANDATORY_KEYS"
      value = "products,mail-templates"
    },
    {
      name  = "TENANT_STRICT_DATA_ISOLATION"
      value = "true"
    },
    {
      name = "SELFCARE_TENANT_DATA_ISOLATION"
      value = jsonencode({
        PNPG = {
          mongo = {
            account                = "cosmos-pnpg"
            database               = module.cosmosdb.database_name
            connectionStringEnvVar = "MONGODB_CONNECTION_STRING_PNPG"
          }
          jwt = {
            publicKeyEnvVar = "JWT_PUBLIC_KEY_PNPG"
          }
          storages = {
            products = {
              account   = data.azurerm_storage_account.product_storage.name
              container = "selc-d-product"
              authentication = {
                type                          = "MANAGED_IDENTITY"
                managedIdentityClientIdEnvVar = "INSTITUTION_PRODUCTS_STORAGE_CLIENT_ID"
              }
            }
            mail-templates = {
              account   = "selcdweupnpgcheckoutst01"
              container = "$web"
              authentication = {
                type                          = "MANAGED_IDENTITY"
                managedIdentityClientIdEnvVar = "INSTITUTION_MAIL_TEMPLATES_STORAGE_CLIENT_ID"
              }
            }
          }
        }
      })
    },
    {
      name  = "DESTINATION_MAILS"
      value = "pectest@pec.pagopa.it"
    },
    {
      name  = "SELFCARE_URL"
      value = "https://selfcare.pagopa.it"
    },
    {
      name  = "LOGO_URL"
      value = "https://pnpg.dev.selfcare.pagopa.it/institutions/"
    },
    {
      name  = "MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH"
      value = "contracts/template/mail/delegation-notification/1.0.1.json"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "institution-ms"
    },
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar -Djava.net.preferIPv4Stack=true -Dnetworkaddress.cache.ttl=30 -Dnetworkaddress.cache.negative.ttl=1"
    },
    {
      name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
      value = "OFF"
    },
    {
      name  = "EXTERNAL_API_LOG_LEVEL"
      value = "DEBUG"
    },
    {
      name  = "SMTP_HOST"
      value = "smtps.pec.aruba.it"
    },
    {
      name  = "SMTP_PORT"
      value = "465"
    },
    {
      name  = "SMTP_SSL"
      value = "true"
    },
    {
      name  = "MS_NOTIFICATION_MANAGER_URL"
      value = "https://selc-${module.local.config.env_short}-pnpg-notification-mngr-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
      value = "https://selc-${module.local.config.env_short}-pnpg-party-reg-proxy-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "USERVICE_USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "SELFCARE_USER_URL"
      value = "https://selc-${module.local.config.env_short}-pnpg-user-ms-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "MAIL_SENDER_ADDRESS"
      value = "noreply@areariservata.pagopa.it"
    },
    {
      name  = "PEC_NOTIFICATION_DISABLED"
      value = "true"
    },
    {
      name  = "INSTITUTION_MAIL_TEMPLATES_STORAGE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.product_storage_blob_identity.client_id
    },
    {
      name  = "INSTITUTION_PRODUCTS_STORAGE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.product_storage_blob_identity.client_id
    },
    {
      name  = "ONE_MAIL_URL"
      value = "https://uat.onemail.pagopa.it"
    },
    {
      name  = "ONE_MAIL_SENDER_ADDRESS"
      value = "noreply@selfcare.pagopa.it"
    },
    {
      name  = "USER_MAIL_ENABLED"
      value = "false"
    }
  ]

  secrets_names_institution_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"    = "appinsights-connection-string"
    "MONGODB_CONNECTION_STRING_PNPG"           = "mongodb-connection-string"
    "SMTP_USR"                                 = "smtp-usr"
    "SMTP_PSW"                                 = "smtp-psw"
    "ONBOARDING_INSTITUTION_ALTERNATIVE_EMAIL" = "party-test-institution-email"
    "USER_REGISTRY_API_KEY"                    = "user-registry-api-key"
    "JWT_PUBLIC_KEY_PNPG"                      = "jwt-public-key"
    "ONE_MAIL_API_KEY"                         = "onemail-api-key"
  }
}
module "container_app_institution_ms_pnpg" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-pnpg-institution-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-institution-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_institution_ms
  secrets_names                  = local.secrets_names_institution_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags
  additional_user_assigned_identity_ids = [
    data.azurerm_user_assigned_identity.product_storage_blob_identity.id
  ]
}
