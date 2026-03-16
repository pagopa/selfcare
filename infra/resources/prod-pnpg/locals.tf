locals {
  prefix         = "selc"
  storage_prefix = "sc"
  env_short      = "p"
  location       = "westeurope"
  location_short = "weu"
  domain         = "pnpg"
  is_pnpg        = true
  # suffix_increment = "-002"

  pnpg_suffix = local.is_pnpg == true ? "-${local.location_short}-${local.domain}" : ""

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${local.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${local.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-account"
    mongodb_name                  = "selcOnboarding"
  }

  container_app_environment_name = "${local.prefix}-${local.env_short}-${local.domain}-cae-cp"
  ca_resource_group_name         = "${local.prefix}-${local.env_short}-container-app-rg"

  function_name = "${local.storage_prefix}-onboarding-fn"

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
    cpu    = 0.5
    memory = "1Gi"
  }

  tags = {
    CreatedBy   = "Terraform"
    Environment = "PROD"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare-onboarding"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  key_vault_resource_group_name = "${local.prefix}-${local.env_short}-${local.domain}-sec-rg"
  key_vault_name                = "${local.prefix}-${local.env_short}-${local.domain}-kv"

  project                  = "${local.prefix}-${local.env_short}"
  resource_group_name_vnet = "${local.project}-vnet-rg"
}
