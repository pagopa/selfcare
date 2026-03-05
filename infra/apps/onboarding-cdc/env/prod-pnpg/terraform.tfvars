prefix    = "selc"
env_short = "p"
is_pnpg   = true

tags = {
  CreatedBy   = "Terraform"
  Environment = "Prod"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare-onboarding"
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
    value = "onboarding-cdc",
  },
  {
    name  = "ONBOARDING-CDC-MONGODB-WATCH-ENABLED"
    value = "false"
  },
  {
    name  = "ONBOARDING_FUNCTIONS_URL"
    value = "https://selc-p-pnpg-onboarding-fn.azurewebsites.net"
  }
]

secrets_names = {
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "MONGODB-CONNECTION-STRING"             = "mongodb-connection-string"
  "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
  "NOTIFICATION-FUNCTIONS-API-KEY"        = "fn-onboarding-primary-key"
}

