###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "uat"
  env_short = "u"
  domain    = "ar"

  dns_zone_prefix                = "uat.selfcare"
  api_dns_zone_prefix            = "api.uat.selfcare"
  private_dns_name_domain        = "mangopond-2a5d4d65.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-u-cae-002"
  ca_resource_group_name         = "selc-u-container-app-002-rg"
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
      value = "https://uat.selfcare.pagopa.it"
    },
    {
      name  = "PUBLIC_FILE_STORAGE_BASE_URL"
      value = "https://selcucheckoutsa.z6.web.core.windows.net"
    },
    {
      name  = "PAGO_PA_BACKOFFICE_URL"
      value = "https://api.uat.platform.pagopa.it/apiconfig/auth/api/v1"
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
      value = "https://selfcare.assistenza.pagopa.it/hc/it/requests/new"
    },
    {
      name  = "SUPPORT_API_ZENDESK_ORGANIZATION"
      value = "_users_hc_selfcare"
    },
    {
      name  = "MS_CORE_URL"
      value = "http://selc-u-institution-ms-ca"
    },
    {
      name  = "USERVICE_PARTY_PROCESS_URL"
      value = "http://selc-u-institution-ms-ca"
    },
    {
      name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
      value = "http://selc-u-party-reg-proxy-ca"
    },
    {
      name  = "MS_USER_GROUP_URL"
      value = "http://selc-u-user-group-ca"
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
      value = "http://selc-u-user-ms-ca"
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
      value = "http://selc-u-onboarding-ms-ca"
    },
    {
      name  = "FEATURE_VIEWCONTRACT_ENABLED"
      value = "true"
    },
    {
      name  = "IAM_URL"
      value = "http://selc-u-iam-ms-ca"
    },
    {
      name  = "DOCUMENT_URL"
      value = "http://selc-u-document-ms-ca"
    }
  ]

  secrets_names_dashboard_bff = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
    "BLOB_STORAGE_CONN_STRING"               = "web-storage-connection-string"
    "USER_REGISTRY_API_KEY"                  = "user-registry-api-key"
    "BACKOFFICE_PAGO_PA_API_KEY"             = "pagopa-backoffice-api-key"
    "SUPPORT_API_KEY"                        = "zendesk-support-api-key"
    "JWT_TOKEN_EXCHANGE_PRIVATE_KEY"         = "jwt-exchange-private-key"
    "JWT_TOKEN_EXCHANGE_KID"                 = "jwt-exchange-kid"
    "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
    "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
    "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"
  }
}

###############################################################################
# Dashboard BFF
###############################################################################

module "container_app_dashboard_bff" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-dashboard-backend"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-dashboard-backend"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_dashboard_bff
  secrets_names                  = local.secrets_names_dashboard_bff
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  tags                           = module.local.config.tags
}

###############################################################################
# APIM
###############################################################################

module "apim_api_bff_dashboard" {
  source              = "../../_modules/apim_api"
  apim_name           = module.local.config.apim_name
  apim_rg             = module.local.config.apim_rg
  api_name            = "selc-${module.local.config.env_short}-api-bff-dashboard"
  display_name        = "BFF Dashboard API"
  base_path           = "dashboard"
  private_dns_name    = "selc-${module.local.config.env_short}-dashboard-backend-ca.${module.local.config.private_dns_name_domain}"
  dns_zone_prefix     = module.local.config.dns_zone_prefix
  api_dns_zone_prefix = module.local.config.api_dns_zone_prefix
  external_domain     = "pagopa.it"
  openapi_path        = "../../../../apps/dashboard-bff/src/main/resources/swagger/api-docs.json"
}
