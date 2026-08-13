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

  tenant_registries = {
    dev = {
      AR = {
        frontend_uri            = "https://dev.selfcare.pagopa.it"
        api_uri                 = "https://api.dev.selfcare.pagopa.it"
        allowed_origins         = ["http://localhost:3000", "https://dev.selfcare.pagopa.it", "https://api.dev.selfcare.pagopa.it"]
        authentication_provider = "ONE_IDENTITY"
        auth_enabled            = true
      }
      PNPG = {
        frontend_uri            = "https://pnpg.dev.selfcare.pagopa.it"
        api_uri                 = "https://api-pnpg.dev.selfcare.pagopa.it"
        allowed_origins         = ["https://pnpg.dev.selfcare.pagopa.it", "https://api-pnpg.dev.selfcare.pagopa.it"]
        authentication_provider = "HUB_SPID_LOGIN"
        auth_enabled            = false
      }
    }
    uat = {
      AR = {
        frontend_uri            = "https://uat.selfcare.pagopa.it"
        api_uri                 = "https://api.uat.selfcare.pagopa.it"
        allowed_origins         = ["https://uat.selfcare.pagopa.it", "https://api.uat.selfcare.pagopa.it"]
        authentication_provider = "ONE_IDENTITY"
        auth_enabled            = true
      }
      PNPG = {
        frontend_uri            = "https://imprese.uat.notifichedigitali.it"
        api_uri                 = "https://api-pnpg.uat.selfcare.pagopa.it"
        allowed_origins         = ["https://imprese.uat.notifichedigitali.it", "https://api-pnpg.uat.selfcare.pagopa.it"]
        authentication_provider = "HUB_SPID_LOGIN"
        auth_enabled            = false
      }
    }
    prod = {
      AR = {
        frontend_uri            = "https://selfcare.pagopa.it"
        api_uri                 = "https://api.selfcare.pagopa.it"
        allowed_origins         = ["https://selfcare.pagopa.it", "https://api.selfcare.pagopa.it"]
        authentication_provider = "ONE_IDENTITY"
        auth_enabled            = true
      }
      PNPG = {
        frontend_uri            = "https://imprese.notifichedigitali.it"
        api_uri                 = "https://api-pnpg.selfcare.pagopa.it"
        allowed_origins         = ["https://imprese.notifichedigitali.it", "https://api-pnpg.selfcare.pagopa.it"]
        authentication_provider = "HUB_SPID_LOGIN"
        auth_enabled            = false
      }
    }
  }

  tenant_registry = local.tenant_registries[local.env]
  tenant_ids = flatten([
    for tenant_id, tenant in local.tenant_registry : [
      for origin in tenant.allowed_origins : {
        id     = tenant_id
        origin = origin
      }
    ]
  ])

  # Tenant identity is derived from the APIM hostname the request arrives on.
  # Unlike Origin/Referer, the hostname is bound to DNS and the TLS certificate,
  # so a caller cannot choose which tenant it is attributed to.
  tenant_hosts = [
    for tenant_id, tenant in local.tenant_registry : {
      id   = tenant_id
      host = regex("^https?://([^/:]+)", tenant.api_uri)[0]
    }
  ]

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
