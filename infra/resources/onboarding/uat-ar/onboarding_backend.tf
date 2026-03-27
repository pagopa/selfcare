###############################################################################
# Onboarding Backend
###############################################################################

locals {
  container_app_onboarding_backend = {
    min_replicas = 1
    max_replicas = 2
    scale_rules = [
      {
        custom = {
          metadata = {
            desiredReplicas = "1"
            start           = "0 8 * * MON-FRI"
            end             = "0 19 * * MON-FRI"
            timezone        = "Europe/Rome"
          }
          type = "cron"
        }
        name = "cron-scale-rule"
      }
    ]
    cpu    = 0.5
    memory = "1Gi"
  }

  app_settings_onboarding_backend = [
    { name = "APPLICATIONINSIGHTS_ROLE_NAME", value = "b4f-onboarding" },
    { name = "JAVA_TOOL_OPTIONS", value = "-javaagent:applicationinsights-agent.jar" },
    { name = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL", value = "OFF" },
    { name = "B4F_ONBOARDING_LOG_LEVEL", value = "DEBUG" },
    { name = "REST_CLIENT_LOGGER_LEVEL", value = "FULL" },
    { name = "MS_ONBOARDING_URL", value = "http://selc-u-onboarding-ms-ca" },
    { name = "MS_CORE_URL", value = "http://selc-u-ms-core-ca" },
    { name = "USERVICE_PARTY_PROCESS_URL", value = "http://selc-u-ms-core-ca" },
    { name = "USERVICE_PARTY_REGISTRY_PROXY_URL", value = "http://selc-u-party-reg-proxy-ca" },
    { name = "USERVICE_USER_REGISTRY_URL", value = "https://api.uat.pdv.pagopa.it/user-registry/v1" },
    { name = "REST_CLIENT_CONNECT_TIMEOUT", value = "60000" },
    { name = "REST_CLIENT_READ_TIMEOUT", value = "60000" },
    { name = "MS_USER_URL", value = "http://selc-u-user-ms-ca" },
    { name = "PRODUCT_STORAGE_CONTAINER", value = "selc-u-product" },
    { name = "ONBOARDING_FUNCTIONS_URL", value = "https://selc-u-onboarding-fn.azurewebsites.net" },
    { name = "MS_USER_INSTITUTION_URL", value = "http://selc-u-user-ms-ca" },
    { name = "MS_PRODUCT_URL", value = "http://selc-u-product-ms-ca" }
  ]

  secrets_names_onboarding_backend = {
    "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
    "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
    "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"
    "ONBOARDING-FUNCTIONS-API-KEY"           = "fn-onboarding-primary-key"
    "USER-ALLOWED-LIST"                      = "user-allowed-list"
  }

  private_dns_name_onboarding_backend = "selc-u-onboardingbackend-ca.mangopond-2a5d4d65.westeurope.azurecontainerapps.io"

  apim_name = "selc-${local.env_short}-apim-v2"
  apim_rg   = "selc-${local.env_short}-api-v2-rg"
}

module "container_app_onboarding_backend" {
  source = "../../_modules/container_app_microservice"

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.container_app_onboarding_backend
  container_app_name             = "onboardingbackend"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-onboarding-backend"
  image_tag                      = local.onboarding_image_tag
  app_settings                   = local.app_settings_onboarding_backend
  secrets_names                  = local.secrets_names_onboarding_backend
  key_vault_resource_group_name  = local.key_vault_resource_group_name
  key_vault_name                 = local.key_vault_name
  probes                         = local.quarkus_health_probes
  tags                           = local.tags
}

###############################################################################
# APIM
###############################################################################

module "apim_api_bff_onboarding" {
  source              = "../../_modules/apim_api"
  apim_name           = local.apim_name
  apim_rg             = local.apim_rg
  api_name            = "selc-${local.env_short}-api-bff-onboarding"
  display_name        = "BFF Onboarding API"
  base_path           = "onboarding"
  private_dns_name    = local.private_dns_name_onboarding_backend
  dns_zone_prefix     = local.dns_zone_prefix
  api_dns_zone_prefix = local.api_dns_zone_prefix
  external_domain     = "pagopa.it"
  openapi_path        = "../../../apps/onboarding-backend/app/src/main/resources/swagger/api-docs.json"
}
