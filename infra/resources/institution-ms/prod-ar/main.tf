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
  container_app_desired_replicas = "3"
  container_app_cpu              = 1.25
  container_app_memory           = "2.5Gi"
}

###############################################################################
# DATA SOURCES
###############################################################################
data "azurerm_storage_account" "product_storage" {
  name                = "selc${module.local.config.env_short}${module.local.config.location_short}archeckoutst01"
  resource_group_name = "selc-${module.local.config.env_short}-checkout-fe-rg"
}

data "azurerm_user_assigned_identity" "product_storage_blob_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-product-storage-blob-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

data "azurerm_storage_account" "documents_storage" {
  name                = "sc${module.local.config.env_short}${module.local.config.location_short}ardocumentsst01"
  resource_group_name = "selc-${module.local.config.env_short}-documents-storage-rg"
}

data "azurerm_user_assigned_identity" "documents_storage_blob_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-documents-storage-blob-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

data "azurerm_user_assigned_identity" "delegations_eventhub_sender_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-sc-delegations-eventhub-sender-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

###############################################################################
# COSMOS DB
###############################################################################

module "cosmosdb" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcMsCore"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  max_throughput              = 20000
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

module "collection_delegations" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "Delegations"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["institutionFromName"], unique = false },
    { keys = ["from"], unique = false },
    { keys = ["to"], unique = false },
    { keys = ["toTaxCode"], unique = false },
    { keys = ["fromTaxCode"], unique = false },
    { keys = ["to", "status", "type", "productId", "createdAt"], unique = false },
    { keys = ["from", "status", "type", "productId", "createdAt"], unique = false },
    { keys = ["createdAt"], unique = false },
    { keys = ["updatedAt"], unique = false }
  ]
}

module "collection_mail_notification" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "MailNotification"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["moduleDayOfTheEpoch"], unique = false },
    { keys = ["institutionId"], unique = true }
  ]
}

###############################################################################
# Institution MS
###############################################################################
locals {
  image_tag = var.image_tag

  app_settings_institution_ms = [
    {
      name  = "TENANT_SUPPORTED_TENANTS"
      value = "AR"
    },
    {
      name  = "TENANT_DEFAULT"
      value = "AR"
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
      value = "false"
    },
    {
      name = "SELFCARE_TENANT_DATA_ISOLATION"
      value = jsonencode({
        AR = {
          mongo = {
            account                = "cosmos-ar"
            database               = module.cosmosdb.database_name
            connectionStringEnvVar = "MONGODB_CONNECTION_STRING_AR"
          }
          jwt = {
            publicKeyEnvVar = "JWT_PUBLIC_KEY_AR"
          }
          storages = {
            products = {
              account   = data.azurerm_storage_account.product_storage.name
              container = "selc-p-product"
              authentication = {
                type                          = "MANAGED_IDENTITY"
                managedIdentityClientIdEnvVar = "INSTITUTION_PRODUCTS_STORAGE_CLIENT_ID"
              }
            }
            mail-templates = {
              account   = "scpweuardocumentsst01"
              container = "sc-p-documents-blob"
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
      name  = "ONBOARDING_SEND_EMAIL_TO_INSTITUTION"
      value = "true"
    },
    {
      name  = "SELFCARE_URL"
      value = "https://selfcare.pagopa.it"
    },
    {
      name  = "LOGO_URL"
      value = "https://selfcare.pagopa.it/institutions/"
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
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
      value = "OFF"
    },
    {
      name  = "EXTERNAL_API_LOG_LEVEL"
      value = "INFO"
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
      value = "https://selc-${module.local.config.env_short}-notification-mngr-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
      value = "https://selc-${module.local.config.env_short}-party-reg-proxy-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "USERVICE_USER_REGISTRY_URL"
      value = "https://api.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "SELFCARE_USER_URL"
      value = "https://selc-${module.local.config.env_short}-user-ms-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "MAIL_SENDER_ADDRESS"
      value = "noreply@areariservata.pagopa.it"
    },
    {
      name  = "PEC_NOTIFICATION_DISABLED"
      value = "false"
    },
    {
      name  = "EVENT_HUB_BASE_PATH"
      value = "https://selc-p-eventhub-ns.servicebus.windows.net/"
    },
    {
      name  = "EVENT_HUB_SC_DELEGATIONS_TOPIC"
      value = "sc-delegations"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "INSTITUTION_MAIL_TEMPLATES_STORAGE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.documents_storage_blob_identity.client_id
    },
    {
      name  = "INSTITUTION_PRODUCTS_STORAGE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.product_storage_blob_identity.client_id
    },
    {
      name  = "EVENTHUB_SENDER_MANAGED_IDENTITY_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.delegations_eventhub_sender_identity.client_id
    },
    {
      name  = "ONE_MAIL_URL"
      value = "https://onemail.pagopa.it"
    },
    {
      name  = "ONE_MAIL_SENDER_ADDRESS"
      value = "noreply@areariservata.pagopa.it"
    },
    {
      name  = "USER_MAIL_ENABLED"
      value = "true"
    }
  ]

  secrets_names_institution_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"    = "appinsights-connection-string"
    "MONGODB_CONNECTION_STRING_AR"             = "mongodb-connection-string"
    "SMTP_USR"                                 = "smtp-usr"
    "SMTP_PSW"                                 = "smtp-psw"
    "ONBOARDING_INSTITUTION_ALTERNATIVE_EMAIL" = "party-test-institution-email"
    "USER_REGISTRY_API_KEY"                    = "user-registry-api-key"
    "JWT_PUBLIC_KEY_AR"                        = "jwt-public-key"
    "ONE_MAIL_API_KEY"                         = "onemail-api-key-prod"
  }
}

module "container_app_institution_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-institution-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-institution-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_institution_ms
  secrets_names                  = local.secrets_names_institution_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags
  additional_user_assigned_identity_ids = [
    data.azurerm_user_assigned_identity.product_storage_blob_identity.id,
    data.azurerm_user_assigned_identity.documents_storage_blob_identity.id,
    data.azurerm_user_assigned_identity.delegations_eventhub_sender_identity.id
  ]
}
