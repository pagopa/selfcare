###############################################################################
# Onboarding Backend - Container App & APIM (DEV PNPG)
###############################################################################

locals {
  container_app_onboarding_backend = {
    min_replicas = 0
    max_replicas = 1
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

  secrets_names_onboarding_backend = {
    "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
    "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
    "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"
    "ONBOARDING-FUNCTIONS-API-KEY"           = "fn-onboarding-primary-key"
  }

  private_dns_name_onboarding_backend = "selc-d-pnpg-onboardingbackend-ca.blackhill-644148c0.westeurope.azurecontainerapps.io"
  apim_name_onboarding_backend       = "selc-${local.env_short}-pnpg-apim-v2"
  apim_rg_onboarding_backend         = "selc-${local.env_short}-pnpg-api-v2-rg"
  dns_zone_prefix_onboarding_backend = "pnpg.dev.selfcare"
  api_dns_zone_prefix_onboarding_backend = "api-pnpg.dev.selfcare"
}

module "container_app_onboarding_backend_pnpg" {
  source = "../_modules/container_app_microservice"

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

resource "azurerm_api_management_api_version_set" "apim_api_bff_onboarding_pnpg" {
  name                = "selc-${local.env_short}-pnpg-api-bff-onboarding"
  resource_group_name = local.apim_rg_onboarding_backend
  api_management_name = local.apim_name_onboarding_backend
  display_name        = "BFF PNPG Onboarding API"
  versioning_scheme   = "Segment"
}

module "apim_api_bff_onboarding_pnpg" {
  source              = "../_modules/apim_api"
  name                = "selc-${local.env_short}-pnpg-api-bff-onboarding"
  api_management_name = local.apim_name_onboarding_backend
  resource_group_name = local.apim_rg_onboarding_backend
  version_set_id      = azurerm_api_management_api_version_set.apim_api_bff_onboarding_pnpg.id

  description  = "BFF PNPG Onboarding API"
  display_name = "BFF PNPG Onboarding API"
  path         = "imprese/onboarding"
  protocols    = ["https"]

  service_url = format("https://%s", local.private_dns_name_onboarding_backend)

  content_format = "openapi+json"
  content_value = templatefile("../../apps/onboarding-backend/app/src/main/resources/swagger/api-docs.json", {
    openapi_title = "selc-pnpg-onboarding"
    url           = format("%s.%s", local.api_dns_zone_prefix_onboarding_backend, local.dns_zone_prefix_onboarding_backend)
    basePath      = "imprese/onboarding"
  })

  subscription_required = false
}
