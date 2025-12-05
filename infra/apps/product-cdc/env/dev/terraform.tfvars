env_short           = "d"
suffix_increment    = "-002"
cae_name            = "cae-002"
dns_zone_prefix     = "dev.selfcare"
api_dns_zone_prefix = "api.dev.selfcare"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Dev"
  Owner       = "SelfCare"
  Source      = "https://github.com/pagopa/selfcare/apps/product-cdc"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}

private_dns_name = "selc-d-product-cdc-ca.whitemoss-eb7ef327.westeurope.azurecontainerapps.io"

container_app = {
  min_replicas = 0
  max_replicas = 1
  scale_rules = [
    {
      custom = {
        metadata = {
          "desiredReplicas" = "1"
          "start"           = "0 8 * * MON-FRI"
          "end"             = "0 20 * * MON-FRI"
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
    value = "product-cdc",
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
    value = true
  },
  {
    name  = "STORAGE_CONTAINER_PRODUCT"
    value = "selc-d-product"
  },
  {
    name  = "TEST"
    value = "1"
  }
]

secrets_names = {
  "BLOB_STORAGE_CONN_STRING_PRODUCT"      = "blob-storage-product-connection-string"
  "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
  "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
  "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
  "JWT_PUBLIC_KEY"                        = "jwt-public-key"
}
