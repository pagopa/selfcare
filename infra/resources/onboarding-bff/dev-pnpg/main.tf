###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-pnpg"
}

###############################################################################
# Onboarding BFF
###############################################################################
locals {
  image_tag = module.local.config.image_tag_latest

  app_settings_onboarding_bff = [
    { name = "APPLICATIONINSIGHTS_ROLE_NAME", value = "b4f-onboarding" },
    { name = "JAVA_TOOL_OPTIONS", value = "-javaagent:applicationinsights-agent.jar" },
    { name = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL", value = "OFF" },
    { name = "B4F_ONBOARDING_LOG_LEVEL", value = "DEBUG" },
    { name = "REST_CLIENT_LOGGER_LEVEL", value = "FULL" },
    { name = "MS_ONBOARDING_URL", value = "http://selc-d-pnpg-onboarding-ms-ca" },
    { name = "MS_CORE_URL", value = "http://selc-d-pnpg-ms-core-ca" },
    { name = "USERVICE_PARTY_PROCESS_URL", value = "http://selc-d-pnpg-ms-core-ca" },
    { name = "USERVICE_PARTY_REGISTRY_PROXY_URL", value = "http://selc-d-pnpg-party-reg-proxy-ca" },
    { name = "USERVICE_USER_REGISTRY_URL", value = "https://api.uat.pdv.pagopa.it/user-registry/v1" },
    { name = "REST_CLIENT_CONNECT_TIMEOUT", value = "60000" },
    { name = "REST_CLIENT_READ_TIMEOUT", value = "60000" },
    { name = "MS_USER_URL", value = "http://selc-d-pnpg-user-ms-ca" },
    { name = "PRODUCT_STORAGE_CONTAINER", value = "selc-d-product" },
    { name = "ONBOARDING_FUNCTIONS_URL", value = "https://selc-d-pnpg-onboarding-fn.azurewebsites.net" },
    { name = "MS_USER_INSTITUTION_URL", value = "http://selc-d-user-ms-ca" },
    { name = "MS_PRODUCT_URL", value = "http://selc-d-product-ms-ca" }
  ]

  secrets_names_onboarding_bff = {
    "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
    "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
    "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"
    "ONBOARDING-FUNCTIONS-API-KEY"           = "fn-onboarding-primary-key"
  }
}
module "container_app_onboarding_bff_pnpg" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-pnpg-onboarding-bff"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-onboarding-bff"
  image_tag                      = local.image_tag
  # image_tag                      = "sha-8f9614e"
  app_settings                  = local.app_settings_onboarding_bff
  secrets_names                 = local.secrets_names_onboarding_bff
  key_vault_resource_group_name = module.local.config.key_vault_resource_group_name
  key_vault_name                = module.local.config.key_vault_name
  tags                          = module.local.config.tags
}

###############################################################################
# APIM
###############################################################################

module "apim_api_bff_onboarding_pnpg" {
  source              = "../../_modules/apim_api"
  apim_name           = module.local.config.apim_name //"selc-${module.local.config.env_short}-pnpg-apim-v2"
  apim_rg             = module.local.config.apim_rg
  api_name            = "selc-${module.local.config.env_short}-pnpg-api-bff-onboarding"
  display_name        = "BFF PNPG Onboarding API"
  base_path           = "imprese/onboarding"
  private_dns_name    = "selc-${module.local.config.env_short}-pnpg-onboarding-bff-ca.${module.local.config.private_dns_name_domain}"
  dns_zone_prefix     = module.local.config.dns_zone_prefix
  api_dns_zone_prefix = module.local.config.api_dns_zone_prefix
  external_domain     = "pagopa.it"
  openapi_path        = "../../../../apps/onboarding-bff/app/src/main/resources/swagger/api-docs.json"
}
