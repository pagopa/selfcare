locals {
  prefix    = "selc"
  env_short = "u"
  env       = "uat"

  storage_state = {
    resource_group_name  = "io-infra-rg"
    storage_account_name = "selc${local.env_short}stinfraterraform"
    container_name       = "azurermstate"
  }

  storage_role = {
    name = "Storage Blob Data Contributor"
  }

  project = "${local.prefix}-${local.env_short}"
}