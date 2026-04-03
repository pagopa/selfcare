###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-ar"
}

module "cosmosdb" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcOnboarding"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_onboardings" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "onboardings"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["createdAt"], unique = false },
    { keys = ["origin"], unique = false },
    { keys = ["originId"], unique = false },
    { keys = ["taxCode"], unique = false },
    { keys = ["subunitCode"], unique = false },
    { keys = ["productId"], unique = false },
    { keys = ["status"], unique = false }
  ]
}

module "collection_tokens" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "tokens"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["createdAt"], unique = false }
  ]
}

resource "random_password" "encryption_key" {
  length  = 32
  special = false

  keepers = {
    version = 1
  }

  lifecycle {
    ignore_changes = all
  }
}

resource "random_password" "encryption_iv" {
  length  = 12
  special = false

  keepers = {
    version = 1
  }

  lifecycle {
    ignore_changes = all
  }
}

resource "azurerm_key_vault_secret" "encryption_iv_secret" {
  name         = "onboarding-data-encryption-iv"
  value        = random_password.encryption_iv.result
  content_type = "text/plain"

  key_vault_id = module.local.key_vault_id

  lifecycle {
    ignore_changes = all
  }
}

resource "azurerm_key_vault_secret" "encryption_key_secret" {
  name         = "onboarding-data-encryption-key"
  value        = random_password.encryption_key.result
  content_type = "text/plain"

  key_vault_id = module.local.key_vault_id

  lifecycle {
    ignore_changes = all
  }
}

locals {
  app_settings_onboarding_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "onboarding-ms"
    },
    {
      name  = "USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "ONBOARDING_FUNCTIONS_URL"
      value = "https://selc-${module.local.config.env_short}-onboarding-fn.azurewebsites.net"
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-${module.local.config.env_short}-product"
    },
    {
      name  = "MS_CORE_URL"
      value = "http://selc-${module.local.config.env_short}-ms-core-ca"
    },
    {
      name  = "MS_PARTY_REGISTRY_URL"
      value = "http://selc-${module.local.config.env_short}-party-reg-proxy-ca"
    },
    {
      name  = "SIGNATURE_VALIDATION_ENABLED"
      value = "false"
    },
    {
      name  = "STORAGE_CONTAINER_CONTRACT"
      value = "sc-${module.local.config.env_short}-documents-blob"
    },
    {
      name  = "MS_USER_URL"
      value = "http://selc-${module.local.config.env_short}-user-ms-ca"
    },
    {
      name  = "ALLOWED_ATECO_CODES"
      value = "47.12.10,47.54.00,47.11.02,47.12.20,47.12.30,47.12.40"
    },
    {
      name  = "PAGOPA_SIGNATURE_SOURCE"
      value = "namirial"
    },
    {
      name  = "NAMIRIAL_BASE_URL"
      value = "https://selc-${module.local.config.env_short}-namirial-sws-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "ONBOARDING-UPDATE-USER-REQUESTER"
      value = "true"
    }
  ]

  secrets_names_onboarding_ms = {
    "JWT-PUBLIC-KEY"                          = "jwt-public-key"
    "JWT_BEARER_TOKEN"                        = "jwt-bearer-token-functions"
    "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
    "USER-REGISTRY-API-KEY"                   = "user-registry-api-key"
    "ONBOARDING-FUNCTIONS-API-KEY"            = "fn-onboarding-primary-key"
    "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"  = "blob-storage-product-connection-string"
    "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "documents-storage-connection-string"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
    "ONBOARDING_DATA_ENCRIPTION_KEY"          = "onboarding-data-encryption-key"
    "ONBOARDING_DATA_ENCRIPTION_IV"           = "onboarding-data-encryption-iv"
    "NAMIRIAL_SIGN_SERVICE_IDENTITY_USER"     = "namirial-sign-service-user"
    "NAMIRIAL_SIGN_SERVICE_IDENTITY_PASSWORD" = "namirial-sign-service-psw"
  }

  container_app_onboarding_cdc = {
    min_replicas = 0
    max_replicas = 1
    scale_rules = [
      {
        custom = {
          metadata = {
            "desiredReplicas" = "1"
            "start"           = "0 8 * * MON-FRI"
            "end"             = "0 19 * * MON-FRI"
            "timezone"        = "Europe/Rome"
          }
          type = "cron"
        }
        name = "cron-scale-rule"
      }
    ]
    cpu    = 1
    memory = "2Gi"
  }

  app_settings_onboarding_cdc = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "onboarding-cdc",
    },
    {
      name  = "ONBOARDING_FUNCTIONS_URL"
      value = "https://selc-${module.local.config.env_short}-onboarding-fn.azurewebsites.net"
    },
    {
      name  = "ONBOARDING-CDC-MONGODB-WATCH-ENABLED"
      value = "true"
    },
    {
      name  = "ONBOARDING-CDC-MINUTES-THRESHOLD-FOR-UPDATE-NOTIFICATION"
      value = "5"
    }
  ]

  secrets_names_onboarding_cdc = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
    "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
    "NOTIFICATION-FUNCTIONS-API-KEY"        = "fn-onboarding-primary-key"
  }

}

module "container_app_onboarding_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-onboarding-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-onboarding-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_onboarding_ms
  secrets_names                  = local.secrets_names_onboarding_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}

