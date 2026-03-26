env_short           = "p"
private_dns_name    = "selc-p-onboardingbackend-ca.lemonpond-bb0b750e.westeurope.azurecontainerapps.io"
dns_zone_prefix     = "selfcare"
api_dns_zone_prefix = "api.selfcare"
suffix_increment    = "-002"
cae_name            = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 1
  max_replicas = 5
  scale_rules = [
    {
      custom = {
        metadata = {
          "desiredReplicas" = "3"
          "start"           = "0 8 * * MON-FRI"
          "end"             = "0 19 * * MON-FRI"
          "timezone"        = "Europe/Rome"
        }
        type = "cron"
      }
      name = "cron-scale-rule"
    }
  ]
  cpu    = 1.25
  memory = "2.5Gi"
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
    value = "INFO",
  },
  {
    name  = "REST_CLIENT_LOGGER_LEVEL"
    value = "BASIC",
  },
  {
    name  = "MS_ONBOARDING_URL"
    value = "http://selc-p-onboarding-ms-ca",
  },
  {
    name  = "MS_CORE_URL"
    value = "http://selc-p-ms-core-ca"
  },
  {
    name  = "USERVICE_PARTY_PROCESS_URL"
    value = "http://selc-p-ms-core-ca"
  },
  {
    name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
    value = "http://selc-p-party-reg-proxy-ca"
  },
  {
    name  = "USERVICE_USER_REGISTRY_URL"
    value = "https://api.pdv.pagopa.it/user-registry/v1"
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
    value = "http://selc-p-user-ms-ca"
  },
  {
    name  = "PRODUCT_STORAGE_CONTAINER"
    value = "selc-p-product"
  },
  {
    name  = "ONBOARDING_FUNCTIONS_URL"
    value = "https://selc-p-onboarding-fn.azurewebsites.net"
  },
  {
    name  = "MS_USER_INSTITUTION_URL"
    value = "http://selc-p-user-ms-ca"
  },
  {
    name  = "MS_PRODUCT_URL"
    value = "http://selc-p-product-ms-ca"
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
