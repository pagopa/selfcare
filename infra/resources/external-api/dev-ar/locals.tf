###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"
  domain    = "ar"

  dns_zone_prefix                = "dev.selfcare"
  api_dns_zone_prefix            = "api.dev.selfcare"
  private_dns_name_domain        = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-cae-002"
  ca_resource_group_name         = "selc-d-container-app-002-rg"
  container_app_min_replicas     = 0
}

locals {
  ca_base_name             = "selc-${module.local.config.env_short}-ext-api-backend"
  monitor_rg_name          = "${module.local.config.prefix}-${module.local.config.env_short}-monitor-rg"
  monitor_appinsights_name = "${module.local.config.prefix}-${module.local.config.env_short}-appinsights"

  container_app = {
    min_replicas = module.local.config.container_app.min_replicas
    max_replicas = module.local.config.container_app.max_replicas
    scale_rules  = module.local.config.container_app.scale_rules
    cpu          = 0.5
    memory       = "1Gi"
  }

  app_settings = [
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "external-api"
    },
    {
      name  = "ALLOWED_SERVICE_TYPES"
      value = "onboarding-interceptor,external-interceptor"
    },
    {
      name  = "REST_CLIENT_READ_TIMEOUT"
      value = "60000"
    },
    {
      name  = "REST_CLIENT_CONNECT_TIMEOUT"
      value = "60000"
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
      name  = "EXTERNAL_API_LOG_LEVEL"
      value = "DEBUG"
    },
    {
      name  = "MS_ONBOARDING_URL"
      value = "http://selc-${module.local.config.env_short}-onboarding-ms-ca"
    },
    {
      name  = "MS_CORE_URL"
      value = "http://selc-${module.local.config.env_short}-institution-ms-ca"
    },
    {
      name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
      value = "http://selc-${module.local.config.env_short}-party-reg-proxy-ca"
    },
    {
      name  = "USERVICE_PARTY_PROCESS_URL"
      value = "http://selc-${module.local.config.env_short}-institution-ms-ca"
    },
    {
      name  = "USERVICE_USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "USERVICE_PARTY_MANAGEMENT_URL"
      value = "http://selc-${module.local.config.env_short}-institution-ms-ca"
    },
    {
      name  = "STORAGE_CONTAINER"
      value = "sc-${module.local.config.env_short}-documents-blob"
    },
    {
      name  = "SELFCARE_USER_URL"
      value = "http://selc-${module.local.config.env_short}-user-ms-ca"
    },
    {
      name  = "MS_DOCUMENT_URL"
      value = "http://selc-${module.local.config.env_short}-document-ms-ca"
    }
  ]

  secrets_names = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
    "BLOB_STORAGE_CONN_STRING"               = "documents-storage-connection-string"
    "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
    "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
    "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"
  }

  probes = [
    {
      httpGet = {
        path   = "/actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      type                = "Liveness"
      failureThreshold    = 3
      initialDelaySeconds = 1
    },
    {
      httpGet = {
        path   = "/actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      type                = "Readiness"
      failureThreshold    = 30
      initialDelaySeconds = 30
    },
    {
      httpGet = {
        path   = "/actuator/health"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 30
      type                = "Startup"
      failureThreshold    = 30
      initialDelaySeconds = 60
    }
  ]
}