###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-pnpg"
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
      value = "https://www.pagopa.gov.it"
    },
    {
      name  = "ACS_BASE_URL"
      value = "https://api-pnpg.dev.selfcare.pagopa.it/spid/v1"
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
      value = "https://pnpg.dev.selfcare.pagopa.it/auth/login/error"
    },
    {
      name  = "ENDPOINT_SUCCESS"
      value = "https://pnpg.dev.selfcare.pagopa.it/auth/login/success"
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
      value = "PagoPA S.p.A."
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
      value = "api-pnpg.dev.selfcare.pagopa.it"
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
      value = "selc-d-logs-blob"
    },
    {
      name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
      value = "OFF"
    },
    {
      name  = "USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "ORG_ISSUER"
      value = "https://selfcare.pagopa.it"
    },
    {
      name  = "CIE_URL"
      value = "https://preproduzione.idserver.servizicie.interno.gov.it/idp/shibboleth?Metadata"
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
      name  = "SPID_TESTENV_URL"
      value = "https://selc-d-pnpg-spid-testenv.westeurope.azurecontainer.io"
    },
    {
      name  = "REDIS_PORT"
      value = "6380"
    },
    {
      name  = "REDIS_URL"
      value = "selc-d-weu-pnpg-redis.redis.cache.windows.net"
    },
    {
      name  = "WELL_KNOWN_URL"
      value = "https://selcdweupnpgcheckoutsa.z6.web.core.windows.net/.well-known/jwks.json"
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
  container_app_name             = "selc-${module.local.config.env_short}-hub-spid-login"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "hub-spid-login-ms"
  app_settings                   = local.app_settings
  secrets_names                  = local.secrets_names
  workload_profile_name          = "Consumption"
  image_tag                      = "5.5.3-RELEASE"
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = local.probes
  tags                           = module.local.config.tags
}
