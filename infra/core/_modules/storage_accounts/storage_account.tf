module "storage_account" {
  source = "pagopa-dx/azure-storage-account/azurerm"
  version = ">= 4.0.5, < 5.0.0"

  subnet_pep_id = azurerm_subnet.storage_account_snet.id
  tags          = var.tags
  use_case            = "default"
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
    "virtual_network_subnet_ids" : [azurerm_subnet.storage_account_snet.id]
  }

  blob_features = var.blob_features

  # -----------------------------------------------------------------------------
  # Microsoft Defender for Storage.
  #
  # pagopa-dx v4.0.5 exposes a single toggle. When true, the upstream module
  # creates `azurerm_security_center_storage_defender.this` with:
  #   - override_subscription_settings_enabled      = true
  #   - malware_scanning_on_upload_enabled          = true
  #   - malware_scanning_on_upload_cap_gb_per_month = -1 (unlimited)
  #   - sensitive_data_discovery_enabled            = true
  # -----------------------------------------------------------------------------
  malware_scanning_enabled = var.malware_scanning_enabled
}

# Lifecycle Management Policy
resource "azurerm_storage_management_policy" "lifecycle" {
  storage_account_id = module.storage_account.id

  # Regola per blob Hot -> Cool -> Archive -> Delete
  rule {
    name    = "lifecycle_rule_privacy"
    enabled = true

    filters {
      prefix_match = var.lifecycle_prefix_match
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

resource "azurerm_key_vault_secret" "storage_connection_string" {
  name         = "${var.app_name}-storage-connection-string"
  value        = module.storage_account.primary_connection_string
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.key_vault.id
}


resource "azurerm_management_lock" "storage_account_lock" {
  name       = module.storage_account.name
  scope      = module.storage_account.id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

################################################################################
# Microsoft Defender for Storage — soft-delete guardrail.
#
# Neither pagopa-dx v4.x nor `azurerm_security_center_storage_defender` exposes
# the "soft-delete malicious blobs" option. This guard only enforces its
# prerequisite (blob soft-delete retention >= 1 day); the option itself must be
# enabled on the Defender malware scanning settings outside this module.
################################################################################

resource "terraform_data" "defender_soft_delete_guard" {
  count = var.defender_soft_delete_malicious_blobs ? 1 : 0

  lifecycle {
    precondition {
      condition     = var.malware_scanning_enabled
      error_message = "defender_soft_delete_malicious_blobs=true requires malware_scanning_enabled=true."
    }
    precondition {
      condition     = var.blob_features.delete_retention_days >= 1
      error_message = "defender_soft_delete_malicious_blobs=true requires blob_features.delete_retention_days >= 1 so that Defender can soft-delete malicious blobs."
    }
  }
}

