module "cosmosdb" {
  source = "../../_modules/cosmosdb_database"

  database_name               = local.mongo_db.database_onboarding_name
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_onboardings" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "onboardings"
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
  database_name               = local.mongo_db.database_onboarding_name

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
  resource_group_name         = local.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = local.mongo_db.cosmosdb_account_mongodb_name
  database_name               = local.mongo_db.database_onboarding_name

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

  key_vault_id = data.azurerm_key_vault.key_vault.id

  lifecycle {
    ignore_changes = all
  }
}

resource "azurerm_key_vault_secret" "encryption_key_secret" {
  name         = "onboarding-data-encryption-key"
  value        = random_password.encryption_key.result
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.key_vault.id

  lifecycle {
    ignore_changes = all
  }
}

locals {
  onboarding_ms_app_settings = [
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
      value = "https://selc-d-pnpg-onboarding-fn.azurewebsites.net"
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-d-product"
    },
    {
      name  = "MS_CORE_URL"
      value = "http://selc-d-pnpg-ms-core-ca"
    },
    {
      name  = "MS_PARTY_REGISTRY_URL"
      value = "http://selc-d-pnpg-party-reg-proxy-ca"
    },
    {
      name  = "SIGNATURE_VALIDATION_ENABLED"
      value = "false"
    },
    {
      name  = "MS_USER_URL"
      value = "http://selc-d-pnpg-user-ms-ca"
    },
    {
      name  = "JWT_BEARER_TOKEN"
      value = "@Microsoft.KeyVault(SecretUri=https://${local.key_vault_name}.vault.azure.net/secrets/jwt-bearer-token-functions/)"
    },
    {
      name  = "ONBOARDING-UPDATE-USER-REQUESTER"
      value = "true"
    }
  ]

  onboarding_ms_secrets_names = {
    "JWT-PUBLIC-KEY"                          = "jwt-public-key"
    "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
    "USER-REGISTRY-API-KEY"                   = "user-registry-api-key"
    "ONBOARDING-FUNCTIONS-API-KEY"            = "fn-onboarding-primary-key"
    "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"  = "blob-storage-product-connection-string"
    "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "blob-storage-contract-connection-string"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
  }

  onboarding_cdc_container_app = {
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

  onboarding_cdc_app_settings = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "onboarding-cdc"
    },
    {
      name  = "ONBOARDING-CDC-MONGODB-WATCH-ENABLED"
      value = "false"
    },
    {
      name  = "ONBOARDING_FUNCTIONS_URL"
      value = "https://selc-d-pnpg-onboarding-fn.azurewebsites.net"
    }
  ]

  onboarding_cdc_secrets_names = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
    "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
    "NOTIFICATION-FUNCTIONS-API-KEY"        = "fn-onboarding-primary-key"
  }

}

module "container_app_onboarding_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.container_app
  container_app_name             = "selc-${local.env_short}-pnpg-onboarding-ms"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-onboarding-ms"
  image_tag                      = local.onboarding_image_tag
  app_settings                   = local.onboarding_ms_app_settings
  secrets_names                  = local.onboarding_ms_secrets_names
  key_vault_resource_group_name  = local.key_vault_resource_group_name
  key_vault_name                 = local.key_vault_name
  probes                         = local.quarkus_health_probes
  tags                           = local.tags
}

module "container_app_onboarding_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.onboarding_cdc_container_app
  container_app_name             = "selc-${local.env_short}-pnpg-onboarding-cdc"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-onboarding-cdc"
  image_tag                      = local.onboarding_image_tag
  app_settings                   = local.onboarding_cdc_app_settings
  secrets_names                  = local.onboarding_cdc_secrets_names
  key_vault_resource_group_name  = local.key_vault_resource_group_name
  key_vault_name                 = local.key_vault_name
  probes                         = local.quarkus_health_probes
  tags                           = local.tags
}

locals {
  onboarding_functions = {
    name                      = "selc-d-pnpg-onboarding-fn"
    subnet_cidr               = ["10.1.152.0/24"]
    always_on                 = false
    service_plan_sku          = "B2"
    service_plan_worker_count = 1
    nat_resource_group_name   = "selc-d-weu-pnpg-nat-rg"
    nat_gateway_name          = "selc-d-weu-pnpg-nat_gw"
    app_settings = {
      "APPLICATIONINSIGHTS_CONNECTION_STRING"              = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/appinsights-connection-string/)"
      "USER_REGISTRY_URL"                                  = "https://api.uat.pdv.pagopa.it/user-registry/v1"
      "MONGODB_CONNECTION_URI"                             = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/mongodb-connection-string/)"
      "USER_REGISTRY_API_KEY"                              = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/user-registry-api-key/)"
      "BLOB_STORAGE_CONN_STRING_PRODUCT"                   = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/blob-storage-product-connection-string/)"
      "STORAGE_CONTAINER_CONTRACT"                         = "selc-d-contracts-blob"
      "STORAGE_CONTAINER_PRODUCT"                          = "selc-d-product"
      "BLOB_STORAGE_CONN_STRING_CONTRACT"                  = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/contracts-storage-blob-connection-string/)"
      "MAIL_DESTINATION_TEST_ADDRESS"                      = "pectest@pec.pagopa.it"
      "MAIL_TEMPLATE_REGISTRATION_REQUEST_PT_PATH"         = "contracts/template/mail/registration-request-pt/1.0.0.json"
      "MAIL_SENDER_ADDRESS"                                = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/smtp-usr/)"
      "MAIL_SERVER_USERNAME"                               = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/smtp-usr/)"
      "MAIL_SERVER_PASSWORD"                               = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/smtp-psw/)"
      "MAIL_SERVER_HOST"                                   = "smtps.pec.aruba.it"
      "MAIL_SERVER_PORT"                                   = "465"
      "MAIL_SERVER_SSL"                                    = "true"
      "MAIL_TEMPLATE_REGISTRATION_NOTIFICATION_ADMIN_PATH" = "contracts/template/mail/registration-notification-admin/1.0.0.json"
      "MAIL_TEMPLATE_NOTIFICATION_PATH"                    = "contracts/template/mail/onboarding-notification/1.0.0.json"
      "ADDRESS_EMAIL_NOTIFICATION_ADMIN"                   = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/portal-admin-operator-email/)"
      "MAIL_TEMPLATE_COMPLETE_PATH"                        = "resources/templates/email/onboarding_1.0.0.json"
      "MAIL_TEMPLATE_AGGREGATE_COMPLETE_PATH"              = "contracts/template/mail/onboarding-complete-aggregate/1.0.0.json"
      "MAIL_TEMPLATE_FD_COMPLETE_NOTIFICATION_PATH"        = "contracts/template/mail/onboarding-complete-fd/1.0.0.json"
      "MAIL_TEMPLATE_AUTOCOMPLETE_PATH"                    = "contracts/template/mail/import-massivo-io/1.0.0.json"
      "MAIL_TEMPLATE_DELEGATION_NOTIFICATION_PATH"         = "contracts/template/mail/delegation-notification/1.0.0.json"
      "MAIL_TEMPLATE_REGISTRATION_PATH"                    = "contracts/template/mail/onboarding-request/1.0.1.json"
      "MAIL_TEMPLATE_REGISTRATION_AGGREGATOR_PATH"         = "contracts/template/mail/onboarding-request-aggregator/1.0.1.json"
      "MAIL_TEMPLATE_REJECT_PATH"                          = "contracts/template/mail/onboarding-refused/1.0.0.json"
      "MAIL_TEMPLATE_PT_COMPLETE_PATH"                     = "contracts/template/mail/registration-complete-pt/1.0.0.json"
      "MAIL_TEMPLATE_REGISTRATION_USER_PATH"               = "contracts/template/mail/onboarding-request-admin/1.0.0.json"
      "MAIL_TEMPLATE_REGISTRATION_USER_NEW_MANAGER_PATH"   = "contracts/template/mail/onboarding-request-manager/1.0.0.json"
      "MAIL_TEMPLATE_USER_COMPLETE_NOTIFICATION_PATH"      = "contracts/template/mail/onboarding-complete-user/1.0.0.json"
      "SELFCARE_ADMIN_NOTIFICATION_URL"                    = "https://imprese.dev.notifichedigitali.it/dashboard/admin/onboarding/"
      "SELFCARE_URL"                                       = "https://imprese.dev.notifichedigitali.it"
      "MAIL_ONBOARDING_CONFIRMATION_LINK"                  = "https://imprese.dev.notifichedigitali.it/onboarding/confirm?jwt="
      "MAIL_USER_CONFIRMATION_LINK"                        = "https://imprese.dev.notifichedigitali.it/onboarding/confirm?add-user=true&jwt="
      "MAIL_ONBOARDING_REJECTION_LINK"                     = "https://imprese.dev.notifichedigitali.it/onboarding/cancel?jwt="
      "MAIL_ONBOARDING_URL"                                = "https://imprese.dev.notifichedigitali.it/onboarding/"
      "MS_USER_URL"                                        = "https://selc-d-pnpg-user-ms-ca.victoriousfield-e39534b8.westeurope.azurecontainerapps.io"
      "MS_CORE_URL"                                        = "https://selc-d-pnpg-ms-core-ca.blackhill-644148c0.westeurope.azurecontainerapps.io"
      "JWT_BEARER_TOKEN"                                   = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/jwt-bearer-token-functions/)"
      "MS_PARTY_REGISTRY_URL"                              = "https://selc-d-pnpg-party-reg-proxy-ca.victoriousfield-e39534b8.westeurope.azurecontainerapps.io"
      "PAGOPA_LOGO_ENABLE"                                 = "false"
      "USER_MS_SEND_MAIL"                                  = "false"
      "FORCE_INSTITUTION_PERSIST"                          = "true"
      "EVENT_HUB_BASE_PATH"                                = "https://selc-d-eventhub-ns.servicebus.windows.net"
      "STANDARD_SHARED_ACCESS_KEY_NAME"                    = "selfcare-wo"
      "EVENTHUB_SC_CONTRACTS_SELFCARE_WO_KEY_LC"           = "string"
      "STANDARD_TOPIC_NAME"                                = "SC-Contracts"
      "SAP_SHARED_ACCESS_KEY_NAME"                         = "external-interceptor-wo"
      "EVENTHUB_SC_CONTRACTS_SAP_SELFCARE_WO_KEY_LC"       = "string"
      "SAP_TOPIC_NAME"                                     = "SC-Contracts-SAP"
      "FD_SHARED_ACCESS_KEY_NAME"                          = "external-interceptor-wo"
      "EVENTHUB_SC_CONTRACTS_FD_SELFCARE_WO_KEY_LC"        = "string"
      "FD_TOPIC_NAME"                                      = "Selfcare-FD"
      "SAP_ALLOWED_INSTITUTION_TYPE"                       = "PA,GSP,SA,AS,SCP"
      "SAP_ALLOWED_ORIGINS"                                = "IPA,SELC"
      "MINUTES_THRESHOLD_FOR_UPDATE_NOTIFICATION"          = "5"
      "EMAIL_SERVICE_AVAILABLE"                            = "FALSE"
      "JWT_TOKEN_ISSUER"                                   = "SPID"
      "JWT_TOKEN_PRIVATE_KEY"                              = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/jwt-private-key/)"
      "JWT_TOKEN_KID"                                      = "@Microsoft.KeyVault(SecretUri=https://selc-d-pnpg-kv.vault.azure.net/secrets/jwt-kid/)"
    }
  }
}

resource "azurerm_resource_group" "onboarding_fn_rg" {
  name     = "${local.onboarding_functions.name}-rg"
  location = local.location

  tags = local.tags
}

resource "azurerm_subnet" "onboarding_fn_snet" {
  name                 = "${local.onboarding_functions.name}-snet"
  resource_group_name  = data.azurerm_virtual_network.vnet_selc.resource_group_name
  virtual_network_name = data.azurerm_virtual_network.vnet_selc.name
  address_prefixes     = local.onboarding_functions.subnet_cidr

  delegation {
    name = "default"

    service_delegation {
      name    = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

resource "azurerm_service_plan" "onboarding_fn_plan" {
  name                = "${local.onboarding_functions.name}-plan"
  location            = azurerm_resource_group.onboarding_fn_rg.location
  resource_group_name = azurerm_resource_group.onboarding_fn_rg.name

  os_type      = "Linux"
  sku_name     = local.onboarding_functions.service_plan_sku
  worker_count = local.onboarding_functions.service_plan_worker_count

  tags = local.tags
}

resource "azurerm_storage_account" "onboarding_fn_storage" {
  name                = replace("${local.onboarding_functions.name}-sa", "-", "")
  location            = azurerm_resource_group.onboarding_fn_rg.location
  resource_group_name = azurerm_resource_group.onboarding_fn_rg.name

  account_kind             = "StorageV2"
  account_tier             = "Standard"
  account_replication_type = "LRS"
  access_tier              = "Hot"

  public_network_access_enabled = true

  tags = local.tags

  lifecycle {
    ignore_changes = [
      public_network_access_enabled,
    ]
  }
}

resource "azurerm_linux_function_app" "onboarding_fn" {
  name                = local.onboarding_functions.name
  location            = azurerm_resource_group.onboarding_fn_rg.location
  resource_group_name = azurerm_resource_group.onboarding_fn_rg.name

  service_plan_id            = azurerm_service_plan.onboarding_fn_plan.id
  storage_account_name       = azurerm_storage_account.onboarding_fn_storage.name
  storage_account_access_key = azurerm_storage_account.onboarding_fn_storage.primary_access_key

  functions_extension_version = "~4"
  virtual_network_subnet_id   = azurerm_subnet.onboarding_fn_snet.id
  https_only                  = true

  identity {
    type = "SystemAssigned"
  }

  site_config {
    always_on              = local.onboarding_functions.always_on
    vnet_route_all_enabled = true

    application_stack {
      java_version = "17"
    }
  }

  app_settings = local.onboarding_functions.app_settings

  tags = local.tags

  lifecycle {
    ignore_changes = [
      app_settings,
    ]
  }
}

resource "azurerm_key_vault_access_policy" "onboarding_fn_keyvault_access_policy" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_linux_function_app.onboarding_fn.identity[0].principal_id

  secret_permissions = [
    "Get",
  ]
}

data "azurerm_function_app_host_keys" "onboarding_fn" {
  name                = azurerm_linux_function_app.onboarding_fn.name
  resource_group_name = azurerm_resource_group.onboarding_fn_rg.name
}

resource "azurerm_key_vault_secret" "onboarding_fn_primary_key" {
  name         = "fn-onboarding-primary-key"
  value        = data.azurerm_function_app_host_keys.onboarding_fn.default_function_key
  content_type = "text/plain"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_resource_group" "onboarding_fn_nat_rg" {
  name = local.onboarding_functions.nat_resource_group_name
}

data "azurerm_nat_gateway" "onboarding_fn_nat_gateway" {
  name                = local.onboarding_functions.nat_gateway_name
  resource_group_name = data.azurerm_resource_group.onboarding_fn_nat_rg.name
}

resource "azurerm_subnet_nat_gateway_association" "onboarding_fn_subnet_nat_gateway" {
  subnet_id      = azurerm_subnet.onboarding_fn_snet.id
  nat_gateway_id = data.azurerm_nat_gateway.onboarding_fn_nat_gateway.id
}
