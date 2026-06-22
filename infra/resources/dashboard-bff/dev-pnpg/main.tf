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
  name                = "selc${module.local.config.env_short}${module.local.config.location_short}${module.local.config.domain}checkoutsa"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-checkout-fe-rg"
}

data "azurerm_user_assigned_identity" "product_storage_blob_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-product-storage-blob-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

data "azurerm_storage_account" "web_storage" {
  name                = "selc${module.local.config.env_short}${module.local.config.location_short}${module.local.config.domain}checkoutst01"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-checkout-fe-rg"
}

data "azurerm_user_assigned_identity" "web_storage_blob_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-web-storage-blob-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

###############################################################################
# Dashboard BFF
###############################################################################
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
      value = "https://pnpg.dev.selfcare.pagopa.it"
    },
    {
      name  = "PUBLIC_FILE_STORAGE_BASE_URL"
      value = "https://selcdweupnpgcheckoutsa.z6.web.core.windows.net"
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
      value = "http://selc-d-pnpg-institution-ms-ca"
    },
    {
      name  = "USERVICE_PARTY_PROCESS_URL"
      value = "http://selc-d-pnpg-institution-ms-ca"
    },
    {
      name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
      value = "http://selc-d-pnpg-party-reg-proxy-ca"
    },
    {
      name  = "MS_USER_GROUP_URL"
      value = "http://selc-d-pnpg-user-group-ca"
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
      value = "http://selc-d-pnpg-user-ms-ca"
    },
    {
      name  = "B4F_DASHBOARD_SECURITY_CONNECTOR"
      value = "v2"
    },
    {
      name  = "ONBOARDING_URL"
      value = "http://selc-d-pnpg-onboarding-ms-ca"
    },
    {
      name  = "PRODUCT_AZURE_STORAGE_ACCOUNT_NAME"
      value = data.azurerm_storage_account.product_storage.name
    },
    {
      name  = "PRODUCT_AZURE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.product_storage_blob_identity.client_id
    },
    {
      name  = "WEB_AZURE_STORAGE_ACCOUNT_NAME"
      value = data.azurerm_storage_account.web_storage.name
    },
    {
      name  = "WEB_AZURE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.web_storage_blob_identity.client_id
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
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags
  additional_user_assigned_identity_ids = [
    data.azurerm_user_assigned_identity.product_storage_blob_identity.id,
    data.azurerm_user_assigned_identity.web_storage_blob_identity.id
  ]
}

###############################################################################
# APIM
###############################################################################

module "apim_api_bff_dashboard_pnpg" {
  source              = "../../_modules/apim_api"
  apim_name           = module.local.config.apim_name //"selc-${module.local.config.env_short}-pnpg-apim-v2"
  apim_rg             = module.local.config.apim_rg
  api_name            = "selc-${module.local.config.env_short}-pnpg-api-bff-dashboard"
  display_name        = "BFF PNPG Dashboard API"
  base_path           = "imprese/dashboard"
  private_dns_name    = "selc-${module.local.config.env_short}-pnpg-dashboard-backend-ca.${module.local.config.private_dns_name_domain}"
  dns_zone_prefix     = module.local.config.dns_zone_prefix
  api_dns_zone_prefix = module.local.config.api_dns_zone_prefix
  external_domain     = "pagopa.it"
  openapi_path        = "../../../../apps/dashboard-bff/src/main/resources/swagger/api-docs.json"
}
