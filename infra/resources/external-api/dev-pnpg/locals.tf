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

locals {
  ca_base_name = "selc-${module.local.config.env_short}-pnpg-ext-api-backend"

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
      value = "-javaagent:applicationinsights-agent.jar -Djava.net.preferIPv4Stack=true -Dnetworkaddress.cache.ttl=30 -Dnetworkaddress.cache.negative.ttl=1"
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
      value = "http://selc-${module.local.config.env_short}-pnpg-onboarding-ms-ca"
    },
    {
      name  = "MS_CORE_URL"
      value = "http://selc-${module.local.config.env_short}-pnpg-institution-ms-ca"
    },
    {
      name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
      value = "http://selc-${module.local.config.env_short}-pnpg-party-reg-proxy-ca"
    },
    {
      name  = "USERVICE_PARTY_PROCESS_URL"
      value = "http://selc-${module.local.config.env_short}-pnpg-institution-ms-ca"
    },
    {
      name  = "USERVICE_USER_REGISTRY_URL"
      value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
    },
    {
      name  = "USERVICE_PARTY_MANAGEMENT_URL"
      value = "http://selc-${module.local.config.env_short}-pnpg-institution-ms-ca"
    },
    {
      name  = "STORAGE_CONTAINER"
      value = "sc-${module.local.config.env_short}-documents-blob"
    },
    {
      name  = "SELFCARE_USER_URL"
      value = "http://selc-${module.local.config.env_short}-pnpg-user-ms-ca"
    },
    {
      name  = "MS_DOCUMENT_URL"
      value = "http://selc-${module.local.config.env_short}-pnpg-document-ms-ca"
    }
  ]

  secrets_names = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
    "BLOB_STORAGE_CONN_STRING"               = "web-storage-connection-string"
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