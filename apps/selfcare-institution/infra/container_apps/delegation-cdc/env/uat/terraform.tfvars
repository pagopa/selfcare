prefix           = "selc"
env_short        = "u"
suffix_increment = "-002"
cae_name         = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-institution"
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
  cpu          = 1
  memory       = "2Gi"
}

app_settings = [
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-javaagent:applicationinsights-agent.jar",
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "institution-cdc",
  },
  {
    name  = "DELEGATION_CDC_SEND_EVENTS_WATCH_ENABLED"
    value = "true"
  },
  {
    name  = "EVENT_HUB_BASE_PATH"
    value = "https://selc-u-eventhub-ns.servicebus.windows.net/"
  },
  {
    name  = "EVENT_HUB_SC_DELEGATIONS_TOPIC"
    value = "sc-delegations"
  },
  {
    name  = "SHARED_ACCESS_KEY_NAME"
    value = "selfcare-wo"
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING"       = "appinsights-connection-string"
  "MONGODB-CONNECTION-STRING"                   = "mongodb-connection-string"
  "STORAGE_CONNECTION_STRING"                   = "blob-storage-product-connection-string"
  "EVENTHUB-SC-DELEGATIONS-SELFCARE-WO-KEY-LC"  = "eventhub-sc-delegations-selfcare-wo-key-lc"
}
