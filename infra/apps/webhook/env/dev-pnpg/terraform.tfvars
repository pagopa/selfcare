env_short           = "d"
is_pnpg             = true
dns_zone_prefix     = "pnpg.dev.selfcare"
api_dns_zone_prefix = "api-pnpg.dev.selfcare"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare/apps/product"
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

workload_profile_name = "Consumption"

app_settings = [
  {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-javaagent:applicationinsights-agent.jar",
  },
  {
    name  = "APPLICATIONINSIGHTS_ROLE_NAME"
    value = "pnpg-webhook",
  },
  {
    name  = "MONGODB_DATABASE_NAME"
    value = "selcProduct"
  },
  {
    name  = "MONGODB_COLLECTION_NAME"
    value = "products"
  },
  {
    name  = "PRODUCT-CDC-MONGODB-WATCH-ENABLED"
    value = false
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-d-product"
  }
]

secrets_names = {
  "BLOB_STORAGE_CONN_STRING_PRODUCT"      = "blob-storage-product-connection-string"
  "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
  "JWT_PUBLIC_KEY"                        = "jwt-public-key"
}

