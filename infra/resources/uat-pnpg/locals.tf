locals {
  prefix         = "sc"
  storage_prefix = "selc"
  env_short      = "u"
  location_short = "weu"
  domain         = "pnpg"

  is_pnpg = true

  pnpg_suffix = local.is_pnpg == true ? "-${local.location_short}-${local.domain}" : ""

  mongo_db = {
    mongodb_rg_name               = "selc-${local.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "selc-${local.env_short}${local.pnpg_suffix}-cosmosdb-mongodb-account"
  }

  tags = {
    CreatedBy   = "Terraform"
    Environment = "UAT"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare-onboarding"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  location = "westeurope"
  # suffix_increment = "-002"

  function_name = "${local.project}-onboarding-fn"

  cidr_subnet_document_storage = ["10.1.136.0/24"]

  key_vault_resource_group_name = "selc-${local.env_short}-sec-rg"
  key_vault_name                = "selc-${local.env_short}-kv"

  project                  = "selc-${local.env_short}"
  naming_config            = "documents"
  resource_group_name_vnet = "${local.project}-vnet-rg"

  cidr_subnet_contract_storage = ["10.1.136.0/24"]

  selc_documents_storage_connection_string = try(
    data.azurerm_key_vault_secret.selc_documents_storage_connection_string.value,
    ""
  )
}
