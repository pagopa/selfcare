env_short = "d"
is_pnpg   = true

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-user"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

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
    value = "user-ms",
  },
  {
    name  = "USER_REGISTRY_URL"
    value = "https://api.uat.pdv.pagopa.it/user-registry/v1"
  },
  {
    name  = "EVENT_HUB_BASE_PATH"
    value = "https://selc-d-eventhub-ns.servicebus.windows.net/sc-users"
  },
  {
    name  = "SHARED_ACCESS_KEY_NAME"
    value = "selfcare-wo"
  },
  {
    name  = "EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC"
    value = "string"
  },
  {
    name  = "USER_MS_RETRY_MIN_BACKOFF"
    value = 5
  },
  {
    name  = "USER_MS_RETRY_MAX_BACKOFF"
    value = 60
  },
  {
    name  = "USER_MS_RETRY"
    value = 3
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
  "JWT-PUBLIC-KEY"                          = "jwt-public-key"
  "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
  "USER-REGISTRY-API-KEY"                   = "user-registry-api-key"
  "AWS-SES-ACCESS-KEY-ID"                   = "aws-ses-access-key-id"
  "AWS-SES-SECRET-ACCESS-KEY"               = "aws-ses-secret-access-key"
  "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"  = "blob-storage-product-connection-string"
  "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "blob-storage-contract-connection-string"
}

