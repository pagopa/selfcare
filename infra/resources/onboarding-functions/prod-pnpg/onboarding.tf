###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env             = "prod"
  env_short       = "p"
  domain          = "pnpg"
  external_domain = "it"

  dns_zone_prefix                = "imprese.notifichedigitali"
  api_dns_zone_prefix            = "api-pnpg.selfcare"
  private_dns_name_domain        = "calmmoss-0be48755.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-p-pnpg-cae-cp"
  ca_resource_group_name         = "selc-p-container-app-rg"
  container_app_max_replicas     = 5
  container_app_desired_replicas = "3"
  container_app_cpu              = 1.25
  container_app_memory           = "2.5Gi"
}

###############################################################################
# ONBOARDING FUNCTIONS
###############################################################################

locals {
  onboarding_functions = {
    name                      = "selc-p-pnpg-onboarding-fn"
    subnet_cidr               = ["10.1.152.0/24"]
    always_on                 = true
    service_plan_sku          = "P1v3"
    service_plan_worker_count = 1
    nat_resource_group_name   = module.local.config.nat_rg_name
    nat_gateway_name          = module.local.config.nat_gw_name
    app_settings = {
      "APPLICATIONINSIGHTS_CONNECTION_STRING"        = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/appinsights-connection-string/)"
      "USER_REGISTRY_URL"                            = "https://api.pdv.pagopa.it/user-registry/v1"
      "MONGODB_CONNECTION_URI"                       = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/mongodb-connection-string/)"
      "USER_REGISTRY_API_KEY"                        = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/user-registry-api-key/)"
      "BLOB_STORAGE_CONN_STRING_PRODUCT"             = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/blob-storage-product-connection-string/)"
      "STORAGE_CONTAINER_PRODUCT"                    = "selc-p-product"
      "BLOB_STORAGE_CONN_STRING_CONTRACT"            = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/blob-storage-contract-connection-string/)"
      "STORAGE_CONTAINER_CONTRACT"                   = "$web"
      "MAIL_DESTINATION_TEST"                        = "false"
      "MAIL_DESTINATION_TEST_ADDRESS"                = "pectest@pec.pagopa.it"
      "MAIL_SENDER_ADDRESS"                          = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/smtp-usr/)"
      "MAIL_SERVER_USERNAME"                         = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/smtp-usr/)"
      "MAIL_SERVER_PASSWORD"                         = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/smtp-psw/)"
      "MAIL_SERVER_HOST"                             = "smtps.pec.aruba.it"
      "MAIL_SERVER_PORT"                             = "465"
      "MAIL_TEMPLATE_COMPLETE_PATH"                  = "resources/templates/email/onboarding_1.0.0.json"
      "MS_USER_URL"                                  = "https://selc-p-pnpg-user-ms-ca.calmmoss-0be48755.westeurope.azurecontainerapps.io"
      "MS_CORE_URL"                                  = "https://selc-p-pnpg-ms-core-ca.calmmoss-0be48755.westeurope.azurecontainerapps.io"
      "JWT_BEARER_TOKEN"                             = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/jwt-bearer-token-functions/)"
      "MS_PARTY_REGISTRY_URL"                        = "https://selc-p-pnpg-party-reg-proxy-ca.calmmoss-0be48755.westeurope.azurecontainerapps.io"
      "PAGOPA_LOGO_ENABLE"                           = "false"
      "RETRY_MAX_ATTEMPTS"                           = "3"
      "FIRST_RETRY_INTERVAL"                         = "5"
      "BACKOFF_COEFFICIENT"                          = "1"
      "EVENT_HUB_BASE_PATH"                          = "https://selc-p-eventhub-ns.servicebus.windows.net"
      "STANDARD_SHARED_ACCESS_KEY_NAME"              = "selfcare-wo"
      "EVENTHUB_SC_CONTRACTS_SELFCARE_WO_KEY_LC"     = "string"
      "STANDARD_TOPIC_NAME"                          = "SC-Contracts"
      "SAP_SHARED_ACCESS_KEY_NAME"                   = "external-interceptor-wo"
      "EVENTHUB_SC_CONTRACTS_SAP_SELFCARE_WO_KEY_LC" = "string"
      "SAP_TOPIC_NAME"                               = "SC-Contracts-SAP"
      "FD_SHARED_ACCESS_KEY_NAME"                    = "external-interceptor-wo"
      "EVENTHUB_SC_CONTRACTS_FD_SELFCARE_WO_KEY_LC"  = "string"
      "FD_TOPIC_NAME"                                = "Selfcare-FD"
      "SAP_ALLOWED_INSTITUTION_TYPE"                 = "PA,GSP,SA,AS,SCP"
      "SAP_ALLOWED_ORIGINS"                          = "IPA,SELC"
      "MINUTES_THRESHOLD_FOR_UPDATE_NOTIFICATION"    = "5"
      "EMAIL_SERVICE_AVAILABLE"                      = "true"
      "JWT_TOKEN_ISSUER"                             = "SPID"
      "JWT_TOKEN_PRIVATE_KEY"                        = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/jwt-private-key/)"
      "JWT_TOKEN_KID"                                = "@Microsoft.KeyVault(SecretUri=https://selc-p-pnpg-kv.vault.azure.net/secrets/jwt-kid/)"
    }
  }
}

module "onboarding_functions" {
  source = "../../_modules/functions"

  functions_name            = local.onboarding_functions.name
  subnet_cidr               = local.onboarding_functions.subnet_cidr
  always_on                 = local.onboarding_functions.always_on
  service_plan_sku          = local.onboarding_functions.service_plan_sku
  service_plan_worker_count = local.onboarding_functions.service_plan_worker_count
  nat_resource_group_name   = local.onboarding_functions.nat_resource_group_name
  nat_gateway_name          = local.onboarding_functions.nat_gateway_name
  vnet_resource_group_name  = module.local.vnet_resource_group_name
  vnet_name                 = module.local.vnet_selc_name
  key_vault_id              = module.local.key_vault_id
  tenant_id                 = module.local.tenant_id
  replication_type          = "LRS"
  app_settings              = local.onboarding_functions.app_settings
  location                  = module.local.config.location
  tags                      = module.local.config.tags
}

data "azurerm_public_ip" "pip_outbound" {
  resource_group_name = module.local.config.nat_rg_name
  name                = "${module.local.config.project}-${module.local.config.pnpg_suffix}-pip-outbound"
}

resource "azurerm_nat_gateway_public_ip_association" "functions_pip_nat_gateway" {
  nat_gateway_id       = module.local.nat_gw_id
  public_ip_address_id = data.azurerm_public_ip.pip_outbound.id
}