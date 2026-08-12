locals {
  # ============================================================
  # Constants
  # ============================================================
  prefix         = "selc"
  storage_prefix = "sc"
  location       = "westeurope"
  location_short = "weu"

  # ============================================================
  # Bridge variables → locals
  # ============================================================
  env_short       = var.env_short
  env             = var.env
  domain          = var.domain
  external_domain = var.external_domain

  dns_zone_prefix     = var.dns_zone_prefix
  api_dns_zone_prefix = var.api_dns_zone_prefix

  private_dns_name_domain        = var.private_dns_name_domain
  container_app_environment_name = var.container_app_environment_name
  ca_resource_group_name         = var.ca_resource_group_name

  # ============================================================
  # Derived values
  # ============================================================
  pnpg_suffix      = "${local.location_short}-${local.domain}"
  project          = "${local.prefix}-${local.env_short}"
  project_location = "${local.prefix}-${local.env_short}-${local.location_short}"

  apim_name = "selc-${local.env_short}-apim-v2"
  apim_rg   = "selc-${local.env_short}-api-v2-rg"

  # CosmosDB resource group and account names differ between ar and pnpg
  mongo_db = local.domain == "pnpg" ? {
    mongodb_rg_name               = "${local.prefix}-${local.env_short}-${local.pnpg_suffix}-cosmosdb-mongodb-rg"
    cosmosdb_account_mongodb_name = "${local.prefix}-${local.env_short}-${local.pnpg_suffix}-cosmosdb-mongodb-account"
    } : {
    mongodb_rg_name               = "${local.prefix}-${local.env_short}-cosmosdb-mongodb-rg"
    cosmosdb_account_mongodb_name = "${local.prefix}-${local.env_short}-cosmosdb-mongodb-account"
  }

  # IAM microservice private DNS entry (domain-aware)
  private_dns_name_ms = {
    private_dns_name_ms = "selc-${local.env_short}${local.domain == "pnpg" ? "-${local.domain}" : ""}-iam-ms-ca.${local.private_dns_name_domain}"
  }

  # Key Vault names include domain suffix for pnpg, not for ar
  key_vault_resource_group_name = local.domain == "pnpg" ? "${local.prefix}-${local.env_short}-${local.domain}-sec-rg" : "${local.prefix}-${local.env_short}-sec-rg"
  key_vault_name                = local.domain == "pnpg" ? "${local.prefix}-${local.env_short}-${local.domain}-kv" : "${local.prefix}-${local.env_short}-kv"

  # ============================================================
  # Multitenant tenant registry (single source of truth).
  # See apps/docs/Multitenant/Step_0/{REQUIREMENTS,ARCHITECTURE}.md (SELC-1, SELC-6): the canonical
  # list of tenants and their frontend origins per environment tier, consumed by every apim_api
  # module call (var.tenant_ids) instead of being repeated per microservice/env. The -pnpg env
  # folders are slated for deprecation once a single -ar deployment per tier serves both tenants;
  # until then this module instance's own domain is listed first. That ordering only decides which
  # tenant local development origins map to; it is NOT a fallback for origin-less requests, which
  # are rejected unless the API sets an explicit apim_api default_tenant_id.
  tenant_frontend_origins = {
    dev = {
      AR   = "https://dev.selfcare.pagopa.it"
      PNPG = "https://pnpg.dev.selfcare.pagopa.it"
    }
    uat = {
      AR   = "https://uat.selfcare.pagopa.it"
      PNPG = "https://imprese.uat.notifichedigitali.it"
    }
    prod = {
      AR   = "https://selfcare.pagopa.it"
      PNPG = "https://imprese.notifichedigitali.it"
    }
  }

  tenant_ids = local.domain == "pnpg" ? [
    { id = "PNPG", origin = local.tenant_frontend_origins[local.env]["PNPG"] },
    { id = "AR", origin = local.tenant_frontend_origins[local.env]["AR"] },
    ] : [
    { id = "AR", origin = local.tenant_frontend_origins[local.env]["AR"] },
    { id = "PNPG", origin = local.tenant_frontend_origins[local.env]["PNPG"] },
  ]

  # Local frontend development origin. CORS on the APIM APIs runs with allow-credentials=true, so
  # allow-listing http://localhost:3000 there means any process listening on port 3000 on a user's
  # machine can issue credentialed calls to that environment. Restricted to dev only.
  local_development_origins = local.env == "dev" ? ["http://localhost:3000"] : []

  resource_group_name_vnet = "${local.project}-vnet-rg"

  # ============================================================
  # Container App
  # ============================================================
  container_app = {
    min_replicas = var.container_app_min_replicas
    max_replicas = var.container_app_max_replicas
    scale_rules = [
      {
        custom = {
          metadata = {
            "desiredReplicas" = var.container_app_desired_replicas
            "start"           = "0 8 * * MON-FRI"
            "end"             = "0 19 * * MON-FRI"
            "timezone"        = "Europe/Rome"
          }
          type = "cron"
        }
        name = "cron-scale-rule"
      }
    ]
    cpu    = var.container_app_cpu
    memory = var.container_app_memory
  }

  # ============================================================
  # Health probes — constant across all environments
  # ============================================================
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

  # ============================================================
  # Tags
  # ============================================================
  tags = {
    CreatedBy   = "Terraform"
    Environment = title(local.env)
    Owner       = "Selfcare"
    Source      = "https://github.com/pagopa/selfcare"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  # ============================================================
  # Networking — constant across all environments
  # ============================================================
  cidr_subnet_document_storage = ["10.1.136.0/24"]
  nat_rg_name                  = coalesce(var.nat_rg_name, "${local.project}-nat-rg")
  nat_gw_name                  = coalesce(var.nat_gw_name, "${local.project}-nat_gw")
  nat_pip_outbound_name        = coalesce(var.nat_pip_outbound_name, "${local.project}-aksoutbound-pip-01")
}
