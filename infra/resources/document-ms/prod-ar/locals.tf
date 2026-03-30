locals {
  environment = {
    prefix         = "selc"
    location       = "westeurope"
    location_short = "weu"
    env_short      = "p"
    env_name       = "prod-ar"
  }

  project = "${local.environment.prefix}-${local.environment.env_short}"

  mongo_db = {
    resource_group_name         = "${local.project}-cosmosdb-mongodb-rg"
    cosmosdb_mongo_account_name = "${local.project}-cosmosdb-mongodb-account"
    database_document_name      = "selcDocument"
  }

  container_app_environment_name = "${local.project}-cae-002"
  ca_resource_group_name         = "${local.project}-container-app-002-rg"

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
    cpu    = 1.25
    memory = "2.5Gi"
  }

  quarkus_health_probes = [
    {
      httpGet = {
        path   = "q/health/live"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      type                = "Liveness"
      failureThreshold    = 3
      initialDelaySeconds = 1
    },
    {
      httpGet = {
        path   = "q/health/ready"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      type                = "Readiness"
      failureThreshold    = 30
      initialDelaySeconds = 3
    },
    {
      httpGet = {
        path   = "q/health/started"
        port   = 8080
        scheme = "HTTP"
      }
      timeoutSeconds      = 5
      failureThreshold    = 5
      type                = "Startup"
      initialDelaySeconds = 5
    }
  ]

  app_settings_document_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "document-ms"
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-p-product"
    },
    {
      name  = "STORAGE_CONTAINER_CONTRACT"
      value = "sc-p-documents-blob"
    }
  ]

  secrets_names_document_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
    "JWT-PUBLIC-KEY"                          = "jwt-public-key"
    "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
    "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"  = "blob-storage-product-connection-string"
    "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "documents-storage-connection-string"
  }

  key_vault_resource_group_name = "${local.project}-sec-rg"
  key_vault_name                = "${local.project}-kv"

  tags = {
    CreatedBy    = "Terraform"
    Environment  = "PROD"
    Owner        = "Selfcare"
    Source       = "https://github.com/pagopa/selfcare/blob/main/infra/resources/document-ms/prod-ar"
    CostCenter   = "TS310 - PAGAMENTI & SERVIZI"
    MicroService = "document"
  }
}
