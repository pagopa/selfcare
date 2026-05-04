env_short           = "p"
private_dns_name    = "selc-p-dashboard-backend-ca.lemonpond-bb0b750e.westeurope.azurecontainerapps.io"
dns_zone_prefix     = "selfcare"
api_dns_zone_prefix = "api.selfcare"
suffix_increment    = "-002"
cae_name            = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-dashboard-backend"
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
    value = "INFO"
  },
  {
    name  = "REST_CLIENT_LOGGER_LEVEL"
    value = "FULL"
  },
  {
    name  = "JWT_TOKEN_EXCHANGE_ISSUER"
    value = "https://selfcare.pagopa.it"
  },
  {
    name  = "PUBLIC_FILE_STORAGE_BASE_URL"
    value = "https://selcpcheckoutsa.z6.web.core.windows.net"
  },
  {
    name  = "PAGO_PA_BACKOFFICE_URL"
    value = "https://api.platform.pagopa.it/apiconfig/auth/api/v1"
  },
  {
    name  = "REST_CLIENT_READ_TIMEOUT"
    value = "45000"
  },
  {
    name  = "REST_CLIENT_CONNECT_TIMEOUT"
    value = "45000"
  },
  {
    name  = "USER_STATES_FILTER"
    value = "ACTIVE,SUSPENDED"
  },
  {
    name  = "SUPPORT_API_ZENDESK_REDIRECT_URI"
    value = "https://selfcare.assistenza.pagopa.it/hc/it/requests/new"
  },
  {
    name  = "SUPPORT_API_ZENDESK_ORGANIZATION"
    value = "_users_hc_selfcare"
  },
  {
    name  = "MS_CORE_URL"
    value = "http://selc-p-institution-ms-ca"
  },
  {
    name  = "USERVICE_PARTY_PROCESS_URL"
    value = "http://selc-p-institution-ms-ca"
  },
  {
    name  = "USERVICE_PARTY_REGISTRY_PROXY_URL"
    value = "http://selc-p-party-reg-proxy-ca"
  },
  {
    name  = "MS_USER_GROUP_URL"
    value = "http://selc-p-user-group-ca"
  },
  {
    name  = "USERVICE_USER_REGISTRY_URL"
    value = "https://api.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "JWT_TOKEN_EXCHANGE_DURATION"
    value = "PT15M"
  },
  {
    name  = "TOKEN_EXCHANGE_BILLING_URL"
    value = "https://portalefatturazione.pagopa.it/auth?selfcareToken=<IdentityToken>"
  },
  {
    name  = "TOKEN_EXCHANGE_BILLING_AUDIENCE"
    value = "portalefatturazione.pagopa.it"
  },
  {
    name  = "SELFCARE_USER_URL"
    value = "http://selc-p-user-ms-ca"
  },
  {
    name  = "B4F_DASHBOARD_SECURITY_CONNECTOR"
    value = "v2"
  },
  {
    name  = "PRODUCT_STORAGE_CONTAINER"
    value = "selc-p-product"
  },
  {
    name  = "ONBOARDING_URL"
    value = "http://selc-p-onboarding-ms-ca"
  },
  {
    name  = "FEATURE_VIEWCONTRACT_ENABLED"
    value = "true"
  },
  {
    name  = "IAM_URL"
    value = "http://selc-p-iam-ms-ca"
  },
  {
    name  = "DOCUMENT_URL"
    value = "http://selc-p-document-ms-ca"
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING"  = "appinsights-connection-string"
  "BLOB_STORAGE_CONN_STRING"               = "web-storage-connection-string"
  "USER_REGISTRY_API_KEY"                  = "user-registry-api-key"
  "BACKOFFICE_PAGO_PA_API_KEY"             = "pagopa-backoffice-api-key"
  "SUPPORT_API_KEY"                        = "zendesk-support-api-key"
  "JWT_TOKEN_EXCHANGE_PRIVATE_KEY"         = "jwt-exchange-private-key"
  "JWT_TOKEN_EXCHANGE_KID"                 = "jwt-exchange-kid"
  "JWT_TOKEN_PUBLIC_KEY"                   = "jwt-public-key"
  "USERVICE_USER_REGISTRY_API_KEY"         = "user-registry-api-key"
  "BLOB_STORAGE_PRODUCT_CONNECTION_STRING" = "blob-storage-product-connection-string"
}