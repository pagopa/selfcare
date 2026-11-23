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
  container_app_max_replicas     = 1
  container_app_desired_replicas = "1"
  container_app_cpu              = 0.5
  container_app_memory           = "1Gi"
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

###############################################################################
# COSMOS DB
###############################################################################


module "cosmosdb" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcUser"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_user_institutions" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "userInstitutions"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  indexes = [
    {
      keys   = ["_id"]
      unique = true
    },
    {
      keys   = ["institutionId"]
      unique = false
    },
    {
      keys   = ["userId", "institutionId"]
      unique = false
    }
  ]
}


###############################################################################
# Container App
###############################################################################

locals {
  tenant_id = "PNPG"
  tenant_registry = {
    (local.tenant_id) = {
      mongo = {
        account                = module.local.config.mongo_db.cosmosdb_account_mongodb_name
        database               = module.cosmosdb.database_name
        connectionStringEnvVar = "MONGODB_CONNECTION_STRING_PNPG"
      }
      jwt          = { publicKeyEnvVar = "JWT_PUBLIC_KEY_PNPG" }
      userRegistry = { apiKeyEnvVar = "USER_REGISTRY_API_KEY_PNPG" }
      storages = {
        products = {
          account   = data.azurerm_storage_account.product_storage.name
          container = "selc-${module.local.config.env_short}-product"
          authentication = {
            type                          = "MANAGED_IDENTITY"
            managedIdentityClientIdEnvVar = "PRODUCTS_STORAGE_MANAGED_IDENTITY_CLIENT_ID_PNPG"
          }
        }
      }
    }
  }

  app_settings_user_ms = [
    { name = "TENANT_SUPPORTED_TENANTS", value = local.tenant_id },
    { name = "TENANT_ENFORCEMENT_ENABLED", value = "true" },
    { name = "TENANT_REGISTRY_JSON", value = jsonencode(local.tenant_registry) },
    { name = "SELFCARE_TENANT_STRICT_DATA_ISOLATION", value = tostring(module.local.config.strict_tenant_data_isolation) },
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar -XX:-UseBASE64Intrinsics -Djava.net.preferIPv4Stack=true -Dnetworkaddress.cache.ttl=30 -Dnetworkaddress.cache.negative.ttl=1"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "user-ms",
    },
    {
      name  = "USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "EVENT_HUB_BASE_PATH"
      value = "https://selc-d-eventhub-ns.servicebus.windows.net/sc-users"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC"
      value = "string"
    },
    {
      name  = "USER_MS_RETRY_MIN_BACKOFF"
      value = 5
    },
    {
      name  = "USER_MS_RETRY_MAX_BACKOFF"
      value = 60
    },
    {
      name  = "USER_MS_RETRY"
      value = 3
    },
    {
      name  = "TENANT_PNPG_SELFCARE_URL"
      value = "https://pnpg.dev.selfcare.pagopa.it"
    },
    {
      name  = "ONBOARDING_URL"
      value = "https://selc-${module.local.config.env_short}-pnpg-onboarding-ms-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "USER_GROUP_URL"
      value = "https://selc-${module.local.config.env_short}-pnpg-user-group-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "WEBHOOK_URL"
      value = "https://selc-${module.local.config.env_short}-webhook-ms-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "TENANT_PNPG_WEBHOOK_ENABLED"
      value = false
    },
    {
      name  = "PRODUCTS_STORAGE_MANAGED_IDENTITY_CLIENT_ID_PNPG"
      value = data.azurerm_user_assigned_identity.product_storage_blob_identity.client_id
    },
    {
      name  = "TENANT_PNPG_ACTIVATE_TEMPLATE_ID"
      value = "selfcare_pnpg_user_activated"
    },
    {
      name  = "TENANT_PNPG_DELETE_TEMPLATE_ID"
      value = "selfcare_pnpg_user_deleted"
    },
    {
      name  = "TENANT_PNPG_SUSPEND_TEMPLATE_ID"
      value = "selfcare_pnpg_user_suspended"
    },
    {
      name  = "TENANT_PNPG_CREATE_SINGLE_ROLE_TEMPLATE_ID"
      value = "selfcare_pnpg_user_added_single_role"
    },
    {
      name  = "ONE_MAIL_URL"
      value = "https://uat.onemail.pagopa.it"
    },
    {
      name  = "TENANT_PNPG_MAIL_ENABLED"
      value = false
    },
    {
      name  = "TENANT_PNPG_MAIL_SENDER",
      value = "noreply@selfcare.pagopa.it"
    }
  ]

  secrets_names_user_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "JWT_PUBLIC_KEY_PNPG"                   = "jwt-public-key"
    "MONGODB_CONNECTION_STRING_PNPG"        = "mongodb-connection-string"
    "USER_REGISTRY_API_KEY_PNPG"            = "user-registry-api-key"
    "AWS-SES-ACCESS-KEY-ID"                 = "aws-ses-access-key-id"
    "AWS-SES-SECRET-ACCESS-KEY"             = "aws-ses-secret-access-key"
    "TENANT_PNPG_ONE_MAIL_API_KEY"          = "onemail-api-key"
  }
}

module "container_app_user_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-${module.local.config.domain}-user-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-user-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_user_ms
  secrets_names                  = local.secrets_names_user_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
  additional_user_assigned_identity_ids = [
    data.azurerm_user_assigned_identity.product_storage_blob_identity.id
  ]
}
