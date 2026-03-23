prefix           = "selc"
env_short        = "p"
suffix_increment = "-002"
cae_name         = "cae-002"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-user"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 1
  max_replicas = 1
  scale_rules  = []
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
    value = "user-group-cdc",
  },
  {
    name  = "EVENT_HUB_BASE_PATH"
    value = "https://selc-p-eventhub-ns.servicebus.windows.net/"
  },
  {
    name  = "EVENT_HUB_SC_USERGROUPS_TOPIC"
    value = "sc-usergroups"
  },
  {
    name  = "SHARED_ACCESS_KEY_NAME"
    value = "selfcare-wo"
  },
  {
    name = "USER_GROUP_CDC_SEND_EVENTS_WATCH_ENABLED"
    value = "true"
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
  "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
  "EVENTHUB-SC-USER-GROUPS-SELFCARE-WO-KEY-LC"  = "eventhub-sc-usergroups-selfcare-wo-key-lc"
}

