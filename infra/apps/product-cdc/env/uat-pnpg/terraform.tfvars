env_short           = "u"
is_pnpg             = true
suffix_increment    = "-001"
cae_name            = "cae-001"
dns_zone_prefix     = "pnpg.uat.selfcare"
api_dns_zone_prefix = "api-pnpg.uat.selfcare"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare/apps/product"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

container_app = {
  min_replicas = 1
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
    value = "pnpg-product-cdc",
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
    value = "selc-u-product"
  }
]

secrets_names = {
  "BLOB_STORAGE_CONN_STRING_PRODUCT"      = "blob-storage-product-connection-string"
  "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
}
