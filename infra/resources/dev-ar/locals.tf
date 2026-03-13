locals {
  prefix         = "selc"
  storage_prefix = "sc"
  env_short      = "d"
  location       = "westeurope"
  location_short = "weu"

  dns_zone_prefix     = "dev.selfcare"
  api_dns_zone_prefix = "api.dev.selfcare"
  # suffix_increment = "-002"

  apim_name = "selc-${local.env_short}-apim-v2"
  apim_rg   = "selc-${local.env_short}-api-v2-rg"

  project = "${local.prefix}-${local.env_short}"

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${local.env_short}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${local.env_short}-cosmosdb-mongodb-account"
    database_onboarding_name      = "selcOnboarding"
    database_auth_name            = "selcAuth"
  }

  private_dns_name_domain = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  private_dns_name_ms = {
    private_dns_name_auth_ms = "selc-d-auth-ms-ca.${local.private_dns_name_domain}"
  }

  function_name = "${local.project}-onboarding-fn"

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
}
