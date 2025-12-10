env_short           = "d"
suffix_increment    = "-002"
cae_name            = "cae-002"
dns_zone_prefix     = "dev.selfcare"
api_dns_zone_prefix = "api.dev.selfcare"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare/apps/product"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

private_dns_name = "selc-d-product-ms-ca.whitemoss-eb7ef327.westeurope.azurecontainerapps.io"

container_app = {
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
    value = "product-ms",
  },
  {
    name  = "SHARED_ACCESS_KEY_NAME"
    value = "selfcare-wo"
  },
  {
    name  = "IAM_MS_RETRY_MIN_BACKOFF"
    value = 5
  },
  {
    name  = "IAM_MS_RETRY_MAX_BACKOFF"
    value = 60
  },
  {
    name  = "IAM_MS_RETRY"
    value = 3
  },
  {
    name  = "MONGODB_DATABASE_NAME"
    value = "selcProduct"
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "SELFCARE_DATA_ENCRIPTION_KEY"          = "selfcare-data-encryption-key"
  "SELFCARE_DATA_ENCRIPTION_IV"           = "selfcare-data-encryption-iv"
  "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
  "JWT_PUBLIC_KEY"                        = "jwt-public-key"
}

