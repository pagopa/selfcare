###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-ar"
}

locals {
  app_settings_onboarding_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "onboarding-ms",
    },
    {
      name  = "USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "ONBOARDING_FUNCTIONS_URL"
      value = "https://selc-d-onboarding-fn.azurewebsites.net"
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-d-product"
    },
    {
      name  = "MS_CORE_URL"
      value = "http://selc-d-ms-core-ca"
    },
    {
      name  = "MS_PARTY_REGISTRY_URL"
      value = "http://selc-d-party-reg-proxy-ca"
    },
    {
      name  = "SIGNATURE_VALIDATION_ENABLED"
      value = "false"
    },
    {
      name  = "MS_USER_URL"
      value = "http://selc-d-user-ms-ca"
    },
    {
      name  = "ALLOWED_ATECO_CODES"
      value = "47.12.10,47.54.00,47.11.02,47.12.20,47.12.30,47.12.40"
    },
    {
      name  = "PAGOPA_SIGNATURE_SOURCE"
      value = "namirial"
    },
    {
      name  = "NAMIRIAL_BASE_URL"
      value = "https://selc-d-namirial-sws-ca.whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
    },
    {
      name  = "ONBOARDING-UPDATE-USER-REQUESTER"
      value = "true"
    }
  ]

  secrets_names_onboarding_ms = {
    "JWT-PUBLIC-KEY"                          = "jwt-public-key"
    "JWT_BEARER_TOKEN"                        = "jwt-bearer-token-functions"
    "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
    "USER-REGISTRY-API-KEY"                   = "user-registry-api-key"
    "ONBOARDING-FUNCTIONS-API-KEY"            = "fn-onboarding-primary-key"
    "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"  = "blob-storage-product-connection-string"
    "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "documents-storage-connection-string"
    "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
    "ONBOARDING_DATA_ENCRIPTION_KEY"          = "onboarding-data-encryption-key"
    "ONBOARDING_DATA_ENCRIPTION_IV"           = "onboarding-data-encryption-iv"
    "NAMIRIAL_SIGN_SERVICE_IDENTITY_USER"     = "namirial-sign-service-user"
    "NAMIRIAL_SIGN_SERVICE_IDENTITY_PASSWORD" = "namirial-sign-service-psw"
  }

  container_app_onboarding_cdc = {
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

  app_settings_onboarding_cdc = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "onboarding-cdc",
    },
    {
      name  = "ONBOARDING_FUNCTIONS_URL"
      value = "https://selc-d-onboarding-fn.azurewebsites.net"
    },
    {
      name  = "ONBOARDING-CDC-MONGODB-WATCH-ENABLED"
      value = "true"
    },
    {
      name  = "ONBOARDING-CDC-MINUTES-THRESHOLD-FOR-UPDATE-NOTIFICATION"
      value = "5"
    }
  ]

  secrets_names_onboarding_cdc = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
    "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
    "NOTIFICATION-FUNCTIONS-API-KEY"        = "fn-onboarding-primary-key"
  }

}

module "container_app_onboarding_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "selc-${module.local.config.env_short}-onboarding-cdc"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-onboarding-cdc"
  image_tag                      = module.local.config.image_tag_latest
  app_settings                   = local.app_settings_onboarding_cdc
  secrets_names                  = local.secrets_names_onboarding_cdc
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}

