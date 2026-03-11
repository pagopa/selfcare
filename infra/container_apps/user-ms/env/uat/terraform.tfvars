env_short        = "u"
suffix_increment = "-002"
cae_name         = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-user"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 1
  max_replicas = 1
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
    value = "https://selc-u-eventhub-ns.servicebus.windows.net/"
  },
  {
    name  = "EVENT_HUB_SC_USERS_TOPIC"
    value = "sc-users"
  },
  {
    name  = "EVENT_HUB_SELFCARE_FD_TOPIC"
    value = "selfcare-fd"
  },
  {
    name  = "SHARED_ACCESS_KEY_NAME"
    value = "selfcare-wo"
  },
  {
    name  = "FD_SHARED_ACCESS_KEY_NAME"
    value = "external-interceptor-wo"
  },
  {
    name  = "USER_MS_EVENTHUB_USERS_ENABLED"
    value = true
  },
  {
    name = "USER_MS_EVENTHUB_SELFCARE_FD_ENABLED"
    value = true
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-u-product"
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
  "APPLICATIONINSIGHTS_CONNECTION_STRING"              = "appinsights-connection-string"
  "JWT-PUBLIC-KEY"                                     = "jwt-public-key"
  "MONGODB-CONNECTION-STRING"                          = "mongodb-connection-string"
  "USER-REGISTRY-API-KEY"                              = "user-registry-api-key"
  "AWS-SES-ACCESS-KEY-ID"                              = "aws-ses-access-key-id"
  "AWS-SES-SECRET-ACCESS-KEY"                          = "aws-ses-secret-access-key"
  "EVENTHUB-SC-USERS-SELFCARE-WO-CONNECTION-STRING-LC" = "eventhub-sc-users-selfcare-wo-connection-string-lc"
  "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"             = "blob-storage-product-connection-string"
  "BLOB-STORAGE-CONTRACT-CONNECTION-STRING"            = "blob-storage-contract-connection-string"
  "EVENTHUB-SC-USERS-SELFCARE-WO-KEY-LC"               = "eventhub-sc-users-selfcare-wo-key-lc"
  "EVENTHUB_SELFCARE_FD_EXTERNAL_KEY_LC"               = "eventhub-selfcare-fd-external-interceptor-wo-key-lc"
}
