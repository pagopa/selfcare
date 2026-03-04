locals {
  prefix    = "sc"
  env_short = "d"
  location  = "westeurope"
  # suffix_increment = "-002"

  function_name = "${local.project}-onboarding-fn"

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Dev"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare-onboarding"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

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
