env_short           = "u"
private_dns_name    = "selc-u-onboardingbackend-ca.mangopond-2a5d4d65.westeurope.azurecontainerapps.io"
dns_zone_prefix     = "uat.selfcare"
api_dns_zone_prefix = "api.uat.selfcare"
suffix_increment    = "-002"
cae_name            = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 1
  max_replicas = 2
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
  cpu    = 0.5
  memory = "1Gi"
}

app_settings = [
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "b4f-onboarding",
  },
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-javaagent:applicationinsights-agent.jar",
  },
  {
    name  = "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"
    value = "OFF",
  },
  {
    name  = "B4F_ONBOARDING_LOG_LEVEL"
    value = "DEBUG",
  },
  {
    name  = "REST_CLIENT_LOGGER_LEVEL"
    value = "FULL",
  },
  {
    name  = "MS_ONBOARDING_URL"
    value = "http://selc-u-onboarding-ms-ca",
  },
  {
    name  = "MS_CORE_URL"
    value = "http://selc-u-ms-core-ca"
  },
  {
    name  = "USERVICE_PARTY_PROCESS_URL"
    value = "http://selc-u-ms-core-ca"
  },
  {
    name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
    value = "http://selc-u-party-reg-proxy-ca"
  },
  {
    name  = "USERVICE_USER_REGISTRY_URL"
    value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "REST_CLIENT_CONNECT_TIMEOUT"
    value = "60000"
  },
  {
    name  = "REST_CLIENT_READ_TIMEOUT"
    value = "60000"
  },
  {
    name  = "MS_USER_URL"
    value = "http://selc-u-user-ms-ca"
  },
  {
    name  = "PRODUCT_STORAGE_CONTAINER"
    value = "selc-u-product"
  },
  {
    name  = "ONBOARDING_FUNCTIONS_URL"
    value = "https://selc-u-onboarding-fn.azurewebsites.net"
  },
  {
    name  = "MS_USER_INSTITUTION_URL"
    value = "http://selc-u-user-ms-ca"
  },
  {
    name  = "MS_PRODUCT_URL"
    value = "http://selc-u-product-ms-ca"
  }
]

secrets_names = {
  "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
  "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
  "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
  "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"
  "ONBOARDING-FUNCTIONS-API-KEY"           = "fn-onboarding-primary-key"
  "USER-ALLOWED-LIST"                      = "user-allowed-list"
}
