resource "azurerm_resource_group" "documents_sa_rg" {
  name = "${local.project}-${local.naming_config}-storage-rg"
  //name = provider::dx::resource_name(merge(local.naming_config, { resource_type = "resource_group" }))
  location = local.location
}

module "storage_documents" {
  source = "../_modules/storage_accounts"

  prefix          = local.prefix
  env_short       = local.env_short
  location        = local.location
  domain          = "ar"
  app_name        = local.naming_config
  instance_number = "01"

  resource_group_name  = azurerm_resource_group.documents_sa_rg.name
  virtual_network_name = data.azurerm_virtual_network.vnet_selc.name

  tags                         = local.tags
  cidr_subnet_contract_storage = local.cidr_subnet_document_storage

  project = local.prefix

  private_dns_zone_resource_group_name = data.azurerm_virtual_network.vnet_selc.resource_group_name

  blob_features = {
    immutability_policy = {
      enabled                       = false
      allow_protected_append_writes = false
      period_since_creation_in_days = 1
    }
    restore_policy_days   = 0 # Cannot enable both immutability_policy and restore_policy
    delete_retention_days = 0
    versioning            = false
    last_access_time      = true
    change_feed = {
      enabled           = false
      retention_in_days = 0
    }
  }

  base_blob_tier_to_cool_after_days_since_modification_greater_than = 1
  base_blob_tier_to_cold_after_days_since_creation_greater_than     = 1
  base_delete_after_days_since_creation_greater_than                = 1

  # snapshot_change_tier_to_archive_after_days_since_creation    = 30
  snapshot_change_tier_to_cool_after_days_since_creation = 1
  snapshot_delete_after_days_since_creation_greater_than = 1

  # version_change_tier_to_archive_after_days_since_creation    = 30
  version_change_tier_to_cool_after_days_since_creation = 1
  version_delete_after_days_since_creation              = 1

  key_vault_resource_group_name = local.key_vault_resource_group_name
  key_vault_name                = local.key_vault_name
}

resource "azurerm_user_assigned_identity" "documents_identity" {
  name                = "${local.project}-${local.naming_config}-identity"
  resource_group_name = azurerm_resource_group.documents_sa_rg.name
  location            = local.location
}

data "azurerm_key_vault_secret" "selc_documents_storage_connection_string" {
  name         = "documents-storage-connection-string"
  key_vault_id = data.azurerm_key_vault.key_vault.id
  depends_on = [module.storage_documents]
}

data "local_file" "resources_logo" {
  filename = "${path.module}/../logo.png"
}

resource "null_resource" "upload_resources_logo" {
  triggers = {
    "changes-in-config" : md5(data.local_file.resources_logo.content)
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload --container '${local.prefix}-${local.env_short}-${local.naming_config}-blob' \
                --connection-string '${local.selc_documents_storage_connection_string}' \
                --file ${data.local_file.resources_logo.filename} \
                --overwrite true \
                --name resources/logo.png
          EOT
  }
}

