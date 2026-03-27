locals {
  prefix         = "selc"
  storage_prefix = "sc"
  env_short      = "d"
  location       = "westeurope"
  location_short = "weu"
  domain         = "pnpg"

  dns_zone_prefix     = "pnpg.dev.selfcare"
  api_dns_zone_prefix = "api-pnpg.dev.selfcare"
  external_domain     = "pagopa.it"

  apim_name      = "selc-${local.env_short}-apim-v2"
  apim_rg        = "selc-${local.env_short}-api-v2-rg"
  apim_base_path = "imprese/iam"
  pnpg_suffix = "${local.location_short}-${local.domain}"

  project = "${local.prefix}-${local.env_short}"

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${local.env_short}-${local.pnpg_suffix}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${local.env_short}-${local.pnpg_suffix}-cosmosdb-mongodb-account"
    database_name                 = "selcIam"
  }

  container_app_environment_name = "${local.prefix}-${local.env_short}-${local.domain}-cae-cp"
  ca_resource_group_name         = "${local.prefix}-${local.env_short}-container-app-rg"

  private_dns_name_domain = "blackhill-644148c0.westeurope.azurecontainerapps.io"
  private_dns_name_ms = {
    private_dns_name_ms = "selc-${local.env_short}-iam-ms-ca.${local.private_dns_name_domain}"
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

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Dev"
    Owner       = "Selfcare"
    Source      = "https://github.com/pagopa/selfcare"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  cidr_subnet_document_storage = ["10.1.136.0/24"]

  key_vault_resource_group_name = "${local.prefix}-${local.env_short}-sec-rg"
  key_vault_name                = "${local.prefix}-${local.env_short}-kv"

  naming_config            = "documents"
  resource_group_name_vnet = "${local.project}-vnet-rg"

  image_tag_latest = "latest"
}
