locals {
  prefix         = "selc"
  storage_prefix = "sc"
  env_short      = "u"
  location       = "westeurope"
  location_short = "weu"
  domain         = "pnpg"

  is_pnpg = true

  pnpg_suffix = local.is_pnpg == true ? "-${local.location_short}-${local.domain}" : ""

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${local.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${local.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-account"
    mongodb_name                  = "selcOnboarding"
  }

  function_name = "${local.storage_prefix}-onboarding-fn"

  tags = {
    CreatedBy   = "Terraform"
    Environment = "UAT"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare-onboarding"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  key_vault_resource_group_name = "${local.prefix}-${local.env_short}-sec-rg"
  key_vault_name                = "${local.prefix}-${local.env_short}-kv"

  project                  = "${local.prefix}-${local.env_short}"
  resource_group_name_vnet = "${local.project}-vnet-rg"
}
