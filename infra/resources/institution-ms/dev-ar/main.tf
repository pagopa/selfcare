###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"
  domain    = "ar"

  dns_zone_prefix                = "dev.selfcare"
  api_dns_zone_prefix            = "api.dev.selfcare"
  private_dns_name_domain        = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-cae-002"
  ca_resource_group_name         = "selc-d-container-app-002-rg"
  container_app_min_replicas     = 0
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
      name  = "DESTINATION_MAILS"
      value = "pectest@pec.pagopa.it"
    },
    {
      name  = "SELFCARE_URL"
      value = "https://selfcare.pagopa.it"
    },
    {
      name  = "LOGO_URL"
      value = "https://dev.selfcare.pagopa.it/institutions/"
    },
    {
      name  = "MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH"
      value = "contracts/template/mail/delegation-notification/1.0.1.json"
    },
    {
      name  = "MAIL_TEMPLATE_DELEGATION_USER_NOTIFICATION_PATH"
      value = "contracts/template/mail/delegation-notification/user-1.0.1.json"
    },
    {
      name  = "STORAGE_CONTAINER"
      value = "sc-d-documents-blob"
    },
    {
      name  = "STORAGE_ENDPOINT"
      value = "core.windows.net"
    },

    {
      name  = "STORAGE_APPLICATION_ID"
      value = "selcdcontractsstorage"
    },

    {
      name  = "STORAGE_CREDENTIAL_ID"
      value = "scdweuardocumentsst01"
    },
    {
      name  = "STORAGE_TEMPLATE_URL"
      value = "https://selcdcheckoutsa.z6.web.core.windows.net"
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
      value = "http://selc-d-notification-mngr-ca"
    },
    {
      name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
      value = "http://selc-d-party-reg-proxy-ca"
    },
    {
      name  = "USERVICE_USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "SELFCARE_USER_URL"
      value = "http://selc-d-user-ms-ca"
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
      value = "https://selc-d-eventhub-ns.servicebus.windows.net/"
    },
    {
      name  = "EVENT_HUB_SC_DELEGATIONS_TOPIC"
      value = "sc-delegations"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    }
  ]

  secrets_names_institution_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"      = "appinsights-connection-string"
    "MONGODB_CONNECTION_URI"                     = "mongodb-connection-string"
    "BLOB_STORAGE_CONN_STRING"                   = "documents-storage-connection-string"
    "SMTP_USR"                                   = "smtp-usr"
    "SMTP_PSW"                                   = "smtp-psw"
    "ONBOARDING_INSTITUTION_ALTERNATIVE_EMAIL"   = "party-test-institution-email"
    "USER_REGISTRY_API_KEY"                      = "user-registry-api-key"
    "JWT_TOKEN_PUBLIC_KEY"                       = "jwt-public-key"
    "BLOB_STORAGE_PRODUCT_CONNECTION_STRING"     = "blob-storage-product-connection-string"
    "AWS_SES_ACCESS_KEY_ID"                      = "aws-ses-access-key-id"
    "AWS_SES_SECRET_ACCESS_KEY"                  = "aws-ses-secret-access-key"
    "EVENTHUB-SC-DELEGATIONS-SELFCARE-WO-KEY-LC" = "eventhub-sc-delegations-selfcare-wo-key-lc"
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
}
