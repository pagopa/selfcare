###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env             = "uat"
  env_short       = "u"
  domain          = "pnpg"
  external_domain = "it"

  dns_zone_prefix                = "imprese.uat.notifichedigitali"
  api_dns_zone_prefix            = "api-pnpg.uat.selfcare"
  private_dns_name_domain        = "orangeground-0bd2d4dc.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-u-pnpg-cae-001"
  ca_resource_group_name         = "selc-u-container-app-001-rg"
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

locals {
  app_settings_dashboard_bff = [
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "b4f-dashboard"
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
      name  = "B4F_ONBOARDING_LOG_LEVEL"
      value = "DEBUG"
    },
    {
      name  = "REST_CLIENT_LOGGER_LEVEL"
      value = "FULL"
    },
    {
      name  = "JWT_TOKEN_EXCHANGE_ISSUER"
      value = "https://pnpg.uat.selfcare.pagopa.it"
    },
    {
      name  = "PUBLIC_FILE_STORAGE_BASE_URL"
      value = "https://selcuweupnpgcheckoutsa.z6.web.core.windows.net"
    },
    {
      name  = "JWT_ISSUER"
      value = "SPID"
    },
    {
      name  = "REST_CLIENT_READ_TIMEOUT"
      value = "30000"
    },
    {
      name  = "REST_CLIENT_CONNECT_TIMEOUT"
      value = "30000"
    },
    {
      name  = "USER_STATES_FILTER"
      value = "ACTIVE,SUSPENDED"
    },
    {
      name  = "SUPPORT_API_ZENDESK_REDIRECT_URI"
      value = "https://send.assistenza.pagopa.it/hc/it/requests/new"
    },
    {
      name  = "SUPPORT_API_ZENDESK_ORGANIZATION"
      value = "_users_hc_send"
    },
    {
      name  = "MS_CORE_URL"
      value = "http://selc-u-pnpg-institution-ms-ca"
    },
    {
      name  = "USERVICE_PARTY_PROCESS_URL"
      value = "http://selc-u-pnpg-institution-ms-ca"
    },
    {
      name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
      value = "http://selc-u-pnpg-party-reg-proxy-ca"
    },
    {
      name  = "MS_USER_GROUP_URL"
      value = "http://selc-u-pnpg-user-group-ca"
    },
    {
      name  = "USERVICE_USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "JWT_TOKEN_EXCHANGE_DURATION"
      value = "PT15M"
    },
    {
      name  = "TOKEN_EXCHANGE_BILLING_URL"
      value = "https://dev.portalefatturazione.pagopa.it/auth?selfcareToken=<IdentityToken>"
    },
    {
      name  = "TOKEN_EXCHANGE_BILLING_AUDIENCE"
      value = "dev.portalefatturazione.pagopa.it"
    },
    {
      name  = "SELFCARE_USER_URL"
      value = "http://selc-u-pnpg-user-ms-ca"
    },
    {
      name  = "B4F_DASHBOARD_SECURITY_CONNECTOR"
      value = "v2"
    },
    {
      name  = "PRODUCT_STORAGE_CONTAINER"
      value = "selc-u-product"
    },
    {
      name  = "ONBOARDING_URL"
      value = "http://selc-u-pnpg-onboarding-ms-ca"
    },
    {
      name  = "AZURE_STORAGE_ACCOUNT_NAME"
      value = data.azurerm_storage_account.product_storage.name
    },
    {
      name  = "AZURE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.product_storage_blob_identity.client_id
    }
  ]

  secrets_names_dashboard_bff = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
    "USER_REGISTRY_API_KEY"                  = "user-registry-api-key"
    "SUPPORT_API_KEY"                        = "zendesk-support-api-key"
    "JWT_TOKEN_EXCHANGE_PRIVATE_KEY"         = "jwt-exchange-private-key"
    "JWT_TOKEN_EXCHANGE_KID"                 = "jwt-exchange-kid"
    "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
    "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
  }
}

###############################################################################
# Dashboard BFF
###############################################################################

module "container_app_dashboard_bff_pnpg" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-pnpg-dashboard-backend"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-dashboard-backend"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_dashboard_bff
  secrets_names                  = local.secrets_names_dashboard_bff
  workload_profile_name          = null
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags
  additional_user_assigned_identity_ids = [data.azurerm_user_assigned_identity.product_storage_blob_identity.id]
}

###############################################################################
# APIM
###############################################################################

module "apim_api_bff_dashboard_pnpg" {
  source              = "../../_modules/apim_api"
  apim_name           = module.local.config.apim_name
  apim_rg             = module.local.config.apim_rg
  api_name            = "selc-${module.local.config.env_short}-pnpg-api-bff-dashboard"
  display_name        = "BFF PNPG Dashboard API"
  base_path           = "imprese/dashboard"
  private_dns_name    = "selc-${module.local.config.env_short}-pnpg-dashboard-backend-ca.${module.local.config.private_dns_name_domain}"
  dns_zone_prefix     = module.local.config.dns_zone_prefix
  api_dns_zone_prefix = module.local.config.api_dns_zone_prefix
  external_domain     = module.local.config.external_domain
  openapi_path        = "../../../../apps/dashboard-bff/src/main/resources/swagger/api-docs.json"
}
