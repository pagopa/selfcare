locals {
  prefix         = "selc"
  storage_prefix = "sc"
  env_short      = "d"
  location       = "westeurope"
  location_short = "weu"
  domain         = "ar"

  dns_zone_prefix     = "dev.selfcare"
  api_dns_zone_prefix = "api.dev.selfcare"
  # suffix_increment = "-002"

  apim_name = "selc-${local.env_short}-apim-v2"
  apim_rg   = "selc-${local.env_short}-api-v2-rg"

  project = "${local.prefix}-${local.env_short}"

  onboarding_image_tag    = var.onboarding_image_tag
  auth_image_tag          = var.auth_image_tag
  product_image_tag       = var.product_image_tag
  product_cdc_image_tag   = var.product_cdc_image_tag
  iam_image_tag           = var.iam_image_tag
  document_image_tag      = var.document_image_tag
  webhook_image_tag       = var.webhook_image_tag
  namirial_sign_image_tag = var.namirial_sign_image_tag

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${local.env_short}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${local.env_short}-cosmosdb-mongodb-account"
    database_onboarding_name      = "selcOnboarding"
    database_auth_name            = "selcAuth"
  }

  container_app_environment_name = "${local.prefix}-${local.env_short}-cae-002"
  ca_resource_group_name         = "${local.prefix}-${local.env_short}-container-app-002-rg"

  private_dns_name_domain = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  private_dns_name_ms = {
    private_dns_name_auth_ms           = "selc-d-auth-ms-ca.${local.private_dns_name_domain}"
    private_dns_name_registry_proxy_ms = "selc-d-party-reg-proxy-ca.${local.private_dns_name_domain}"

  }

  function_name = "${local.project}-onboarding-fn"

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

  cidr_subnet_contract_storage = ["10.1.136.0/24"]

  selc_documents_storage_connection_string = try(
    data.azurerm_key_vault_secret.selc_documents_storage_connection_string.value,
    ""
  )

  image_tag_latest = "latest"
}
