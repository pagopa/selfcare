prefix           = "selc"
env_short        = "p"
suffix_increment = "-002"
cae_name         = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
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
  cpu    = 0.5
  memory = "1Gi"
}

app_settings = [
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
    value = "https://api.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "ONBOARDING_FUNCTIONS_URL"
    value = "https://selc-p-onboarding-fn.azurewebsites.net"
  },
  {
    name  = "ONBOARDING_ALLOWED_INSTITUTIONS_PRODUCTS"
    value = "prod-interop,prod-pn,prod-io,prod-io-premium,prod-pagopa,prod-dashboard-psp,prod-sendino,prod-io-sign,prod-registro-beni,prod-idpay-merchant,prod-idpay-gi"
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-p-product"
  },
  {
    name  = "MS_CORE_URL"
    value = "http://selc-p-ms-core-ca"
  },
  {
    name  = "MS_PARTY_REGISTRY_URL"
    value = "http://selc-p-party-reg-proxy-ca"
  },
  {
    name  = "STORAGE_CONTAINER_CONTRACT"
    value = "sc-p-documents-blob"
  },
  {
    name  = "MS_USER_URL"
    value = "http://selc-p-user-ms-ca"
  },
  {
    name  = "ALLOWED_ATECO_CODES"
    value = "47.12.10,47.54.00,47.11.02,47.12.20,47.12.30,47.12.40"
  },
  {
    name  = "ONBOARDING-UPDATE-USER-REQUESTER"
    value = "true",
  }
]

secrets_names = {
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
}
