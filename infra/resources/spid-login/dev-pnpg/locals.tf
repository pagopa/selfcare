locals {
  prefix         = "selc"
  storage_prefix = "sc"
  env_short      = "d"
  location       = "westeurope"
  location_short = "weu"
  domain         = "pnpg"

  dns_zone_prefix     = "pnpg.dev.selfcare"
  api_dns_zone_prefix = "api-pnpg.dev.selfcare"

  pnpg_suffix = "${local.location_short}-${local.domain}"

  project = "${local.prefix}-${local.env_short}"

  container_app_environment_name = "${local.prefix}-${local.env_short}-pnpg-cae-cp"
  ca_resource_group_name         = "${local.prefix}-${local.env_short}-container-app-rg"

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

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Dev"
    Owner       = "Selfcare"
    Source      = "https://github.com/pagopa/selfcare"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  key_vault_resource_group_name = "${local.prefix}-${local.env_short}-${local.domain}-sec-rg"
  key_vault_name                = "${local.prefix}-${local.env_short}-${local.domain}-kv"

  resource_group_name_vnet = "${local.project}-vnet-rg"
}
