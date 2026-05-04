is_pnpg             = true
env_short           = "u"
private_dns_name    = "selc-u-pnpg-dashboard-backend-ca.orangeground-0bd2d4dc.westeurope.azurecontainerapps.io"
dns_zone_prefix     = "imprese.uat.notifichedigitali"
api_dns_zone_prefix = "api-pnpg.uat.selfcare"
external_domain     = "it"
suffix_increment    = "-001"
cae_name            = "cae-001"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-dashboard-backend"
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
    value = "b4f-dashboard"
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
    name  = "B4F_ONBOARDING_LOG_LEVEL"
    value = "DEBUG"
  },
  {
    name  = "REST_CLIENT_LOGGER_LEVEL"
    value = "FULL"
  },
  {
    name  = "JWT_TOKEN_EXCHANGE_ISSUER"
    value = "https://pnpg.uat.selfcare.pagopa.it"
  },
  {
    name  = "PUBLIC_FILE_STORAGE_BASE_URL"
    value = "https://selcuweupnpgcheckoutsa.z6.web.core.windows.net"
  },
  {
    name  = "JWT_ISSUER"
    value = "SPID"
  },
  {
    name  = "REST_CLIENT_READ_TIMEOUT"
    value = "30000"
  },
  {
    name  = "REST_CLIENT_CONNECT_TIMEOUT"
    value = "30000"
  },
  {
    name  = "USER_STATES_FILTER"
    value = "ACTIVE,SUSPENDED"
  },
  {
    name  = "SUPPORT_API_ZENDESK_REDIRECT_URI"
    value = "https://send.assistenza.pagopa.it/hc/it/requests/new"
  },
  {
    name  = "SUPPORT_API_ZENDESK_ORGANIZATION"
    value = "_users_hc_send"
  },
  {
    name  = "MS_CORE_URL"
    value = "http://selc-u-pnpg-institution-ms-ca"
  },
  {
    name  = "USERVICE_PARTY_PROCESS_URL"
    value = "http://selc-u-pnpg-institution-ms-ca"
  },
  {
    name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
    value = "http://selc-u-pnpg-party-reg-proxy-ca"
  },
  {
    name  = "MS_USER_GROUP_URL"
    value = "http://selc-u-pnpg-user-group-ca"
  },
  {
    name  = "USERVICE_USER_REGISTRY_URL"
    value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "JWT_TOKEN_EXCHANGE_DURATION"
    value = "PT15M"
  },
  {
    name  = "TOKEN_EXCHANGE_BILLING_URL"
    value = "https://dev.portalefatturazione.pagopa.it/auth?selfcareToken=<IdentityToken>"
  },
  {
    name  = "TOKEN_EXCHANGE_BILLING_AUDIENCE"
    value = "dev.portalefatturazione.pagopa.it"
  },
  {
    name  = "SELFCARE_USER_URL"
    value = "http://selc-u-pnpg-user-ms-ca"
  },
  {
    name  = "B4F_DASHBOARD_SECURITY_CONNECTOR"
    value = "v2"
  },
  {
    name  = "PRODUCT_STORAGE_CONTAINER"
    value = "selc-u-product"
  },
  {
    name  = "ONBOARDING_URL"
    value = "http://selc-u-pnpg-onboarding-ms-ca"
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
  "BLOB_STORAGE_CONN_STRING"               = "web-storage-connection-string"
  "USER_REGISTRY_API_KEY"                  = "user-registry-api-key"
  "SUPPORT_API_KEY"                        = "zendesk-support-api-key"
  "JWT_TOKEN_EXCHANGE_PRIVATE_KEY"         = "jwt-exchange-private-key"
  "JWT_TOKEN_EXCHANGE_KID"                 = "jwt-exchange-kid"
  "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
  "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
  "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"
}
