is_pnpg          = true
env_short        = "u"
suffix_increment = "-001"
cae_name         = "cae-001"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-external-api-backend"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 1
  max_replicas = 2
  scale_rules  = [
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
    value = "http://selc-u-pnpg-onboarding-ms-ca"
  },
  {
    name  = "MS_CORE_URL"
    value = "http://selc-u-pnpg-institution-ms-ca"
  },
  {
    name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
    value = "http://selc-u-pnpg-party-reg-proxy-ca"
  },
  {
    name  = "USERVICE_PARTY_PROCESS_URL"
    value = "http://selc-u-pnpg-institution-ms-ca"
  },
  {
    name  = "USERVICE_USER_REGISTRY_URL"
    value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "USERVICE_PARTY_MANAGEMENT_URL"
    value = "http://selc-u-pnpg-institution-ms-ca"
  },
  {
    name  = "SELFCARE_USER_URL"
    value = "http://selc-u-pnpg-user-ms-ca"
  },
  {
    name  = "PRODUCT_STORAGE_CONTAINER"
    value = "selc-u-product"
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
  "BLOB_STORAGE_CONN_STRING"               = "web-storage-connection-string"
  "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
  "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
  "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"

}
