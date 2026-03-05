module "storage_account" {
  source  = "pagopa-dx/azure-storage-account/azurerm"
  version = "~>1.0"

  subnet_pep_id       = azurerm_subnet.documents_snet.id
  tags                = var.tags
  tier                = "l"
  environment         = local.environment
  resource_group_name = var.resource_group_name

  subservices_enabled = {
    blob  = true
    file  = false
    queue = false
    table = false
  }

  private_dns_zone_resource_group_name = var.private_dns_zone_resource_group_name
  network_rules = {
    "bypass" : [],
    "default_action" : "Deny",
    "ip_rules" : [],
    "virtual_network_subnet_ids" : [azurerm_subnet.documents_snet.id]
  }

  blob_features = var.blob_features
}

# Lifecycle Management Policy
resource "azurerm_storage_management_policy" "lifecycle" {
  storage_account_id = module.storage_account.id

  # Regola per blob Hot -> Cool -> Archive -> Delete
  rule {
    name    = "lifecycle_rule_privacy"
    enabled = true

    filters {
      prefix_match = ["parties/deleted"]
      blob_types   = ["blockBlob"]
    }

    actions {
      base_blob {
        tier_to_cool_after_days_since_modification_greater_than = var.base_blob_tier_to_cool_after_days_since_modification_greater_than
        tier_to_cold_after_days_since_creation_greater_than     = var.base_blob_tier_to_cold_after_days_since_creation_greater_than
        delete_after_days_since_creation_greater_than           = var.base_delete_after_days_since_creation_greater_than
      }

      snapshot {
        change_tier_to_cool_after_days_since_creation = var.snapshot_change_tier_to_cool_after_days_since_creation
        delete_after_days_since_creation_greater_than = var.snapshot_delete_after_days_since_creation_greater_than
      }

      version {
        change_tier_to_cool_after_days_since_creation = var.version_change_tier_to_cool_after_days_since_creation
        delete_after_days_since_creation              = var.version_delete_after_days_since_creation
      }
    }
  }
}

resource "azurerm_key_vault_secret" "selc_documents_storage_connection_string" {
  name         = "documents-storage-connection-string"
  value        = module.storage_account.primary_connection_string
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.key_vault.id
}


resource "azurerm_management_lock" "selc_documents_storage_management_lock" {
  name       = module.storage_account.name
  scope      = module.storage_account.id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}