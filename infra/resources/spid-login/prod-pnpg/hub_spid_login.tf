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
# Container App
###############################################################################


locals {
  app_settings = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "hub-spid-login-ms",
    },
    {
      name  = "ORG_URL"
      value = "https://www.pagopa.it"
    },
    {
      name  = "ACS_BASE_URL"
      value = "https://api-pnpg.selfcare.pagopa.it/spid/v1"
    },
    {
      name  = "ORG_DISPLAY_NAME"
      value = "PagoPA S.p.A"
    },
    {
      name  = "ORG_NAME"
      value = "PagoPA"
    },
    {
      name  = "AUTH_N_CONTEXT"
      value = "https://www.spid.gov.it/SpidL2"
    },
    {
      name  = "ENDPOINT_ACS"
      value = "/acs"
    },
    {
      name  = "ENDPOINT_ERROR"
      value = "https://imprese.notifichedigitali.it/auth/login/error"
    },
    {
      name  = "ENDPOINT_SUCCESS"
      value = "https://imprese.notifichedigitali.it/auth/login/success"
    },
    {
      name  = "ENDPOINT_LOGIN"
      value = "/login"
    },
    {
      name  = "ENDPOINT_METADATA"
      value = "/metadata"
    },
    {
      name  = "ENDPOINT_LOGOUT"
      value = "/logout"
    },
    {
      name  = "SPID_ATTRIBUTES"
      value = "name,familyName,fiscalNumber"
    },
    {
      name  = "SPID_VALIDATOR_URL"
      value = "https://validator.spid.gov.it"
    },
    {
      name  = "REQUIRED_ATTRIBUTES_SERVICE_NAME"
      value = "Portale Notifiche Digitali Imprese"
    },
    {
      name  = "ENABLE_FULL_OPERATOR_METADATA"
      value = "true"
    },
    {
      name  = "COMPANY_EMAIL"
      value = "pagopaspa@pec.pagopa.it"
    },
    {
      name  = "COMPANY_FISCAL_CODE"
      value = "15376371009"
    },
    {
      name  = "COMPANY_IPA_CODE"
      value = "PagoPA3"
    },
    {
      name  = "COMPANY_NAME"
      value = "PagoPA"
    },
    {
      name  = "COMPANY_VAT_NUMBER"
      value = "IT15376371009"

    },
    {
      name  = "ENABLE_JWT"
      value = "true"
    },
    {
      name  = "INCLUDE_SPID_USER_ON_INTROSPECTION"
      value = "true"
    },
    {
      name  = "TOKEN_EXPIRATION"
      value = 32400
    },
    {
      name  = "JWT_TOKEN_ISSUER"
      value = "SPID"
    },
    {
      name  = "ENABLE_ADE_AA"
      value = "false"
    },
    {
      name  = "APPINSIGHTS_DISABLED"
      value = "false"
    },
    {
      name  = "ENABLE_USER_REGISTRY"
      value = "true"
    },
    {
      name  = "JWT_TOKEN_AUDIENCE"
      value = "imprese.notifichedigitali.it"
    },
    {
      name  = "ENABLE_SPID_ACCESS_LOGS"
      value = "true"
    },
    {
      name  = "SPID_LOGS_STORAGE_KIND"
      value = "azurestorage"
    },
    {
      name  = "SPID_LOGS_STORAGE_CONTAINER_NAME"
      value = "selc-p-weu-pnpg-logs-blob"
    },
    {
      name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
      value = "OFF"
    },
    {
      name  = "USER_REGISTRY_URL"
      value = "https://api.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "ORG_ISSUER"
      value = "https://imprese.notifichedigitali.it"
    },
    {
      name  = "CIE_URL"
      value = "https://api.is.eng.pagopa.it/idp-keys/cie/latest"
    },
    {
      name  = "SERVER_PORT"
      value = "8080"
    },
    {
      name  = "IDP_METADATA_URL"
      value = "https://api.is.eng.pagopa.it/idp-keys/spid/latest"
    },
    {
      name  = "REDIS_PORT"
      value = "6380"
    },
    {
      name  = "REDIS_URL"
      value = "selc-p-weu-pnpg-redis.redis.cache.windows.net"
    },
    {
      name  = "WELL_KNOWN_URL"
      value = "https://selcpweupnpgcheckoutsa.z6.web.core.windows.net/.well-known/jwks.json"
    }
  ]

  secrets_names = {
    "SPID_LOGS_PUBLIC_KEY"                  = "spid-logs-encryption-public-key"
    "REDIS_PASSWORD"                        = "redis-primary-access-key"
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "APPINSIGHTS_INSTRUMENTATIONKEY"        = "appinsights-instrumentation-key"
    "JWT_TOKEN_PRIVATE_KEY"                 = "jwt-private-key"
    "JWT_TOKEN_KID"                         = "jwt-kid"
    "METADATA_PUBLIC_CERT"                  = "agid-spid-cert"
    "METADATA_PRIVATE_CERT"                 = "agid-spid-private-key"
    "USER_REGISTRY_API_KEY"                 = "user-registry-api-key"
    "SPID_LOGS_STORAGE_CONNECTION_STRING"   = "logs-storage-connection-string"
  }

  probes = [
    {
      httpGet = {
        path   = "/info"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      type                = "Liveness"
      failureThreshold    = 5
      initialDelaySeconds = 1
    },
    {
      httpGet = {
        path   = "/info"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      type                = "Readiness"
      failureThreshold    = 3
      initialDelaySeconds = 3
    },
    {
      httpGet = {
        path   = "/info"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      failureThreshold    = 30
      type                = "Startup"
      initialDelaySeconds = 30
    }
  ]

}

module "container_app_hub_spid_login" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-${module.local.config.domain}-hub-spid-login"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "hub-spid-login-ms"
  app_settings                   = local.app_settings
  secrets_names                  = local.secrets_names
  workload_profile_name          = null
  image_tag                      = "5.5.3-RELEASE"
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = local.probes
  tags                           = module.local.config.tags
}
