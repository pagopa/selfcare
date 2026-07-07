################################################################################
# Storage Account (dedicated to end-user uploaded attachments — "sandbox")
#
# Public network access is denied by default (network_rules.default_action = "Deny")
# and the account is reachable only through the dedicated Private Endpoint / subnet.
# This satisfies the security team recommendation of keeping the sandbox storage
# unreachable from public networks to avoid race conditions on malicious blobs.
################################################################################

module "storage_account" {
  source  = "pagopa-dx/azure-storage-account/azurerm"
  version = "~>1.0"

  subnet_pep_id       = azurerm_subnet.user_attachments_snet.id
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
    "virtual_network_subnet_ids" : [azurerm_subnet.user_attachments_snet.id]
  }

  blob_features = var.blob_features
}

################################################################################
# Lifecycle Management Policy
# Applies tier-down + delete only to blobs matching var.lifecycle_prefix_match.
################################################################################

resource "azurerm_storage_management_policy" "lifecycle" {
  storage_account_id = module.storage_account.id

  rule {
    name    = "lifecycle_rule_user_attachments"
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

################################################################################
# Key Vault secret — connection string (consumed by document-ms container app).
################################################################################

resource "azurerm_key_vault_secret" "selc_user_attachments_storage_connection_string" {
  name         = "user-attachments-storage-connection-string"
  value        = module.storage_account.primary_connection_string
  content_type = "text/plain"

  key_vault_id = data.azurerm_key_vault.key_vault.id
}

################################################################################
# Management lock on the storage account.
################################################################################

resource "azurerm_management_lock" "selc_user_attachments_storage_management_lock" {
  name       = module.storage_account.name
  scope      = module.storage_account.id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

################################################################################
# Defender for Storage
#
# Enabled per-storage-account (see security team recommendation:
# "ProdSec-Azure Defender for Storage ACB").
#
# - Activity Monitoring: always enabled when Defender is on.
# - On-upload Malware Scanning: enabled by default; the outcome is written as a
#   blob index tag on the file.
# - Soft-delete of malicious blobs: enabled by default to actively block
#   distribution of files flagged as malicious (requires blob soft-delete
#   retention to be configured in blob_features).
################################################################################

resource "azurerm_security_center_storage_defender" "selc_user_attachments_defender" {
  count = var.defender_enabled ? 1 : 0

  storage_account_id                                = module.storage_account.id
  override_subscription_settings_enabled            = true
  malware_scanning_on_upload_enabled                = var.defender_malware_scanning_enabled
  malware_scanning_on_upload_cap_gb_per_month       = var.defender_malware_scanning_cap_gb_per_month
  sensitive_data_discovery_enabled                  = var.defender_sensitive_data_discovery_enabled
  scan_results_event_grid_topic_id                  = null
}

# NOTE on "soft-delete malicious blobs":
# In the Azure portal this is configured on the storage account's
# "Microsoft Defender for Cloud > Settings" pane; at the time of writing there
# is no first-class azurerm attribute for it. The behaviour depends on the
# blob soft-delete retention configured via `blob_features.delete_retention_days`.
# We surface the toggle (`defender_soft_delete_malicious_blobs`) as a placeholder
# — when true, callers MUST also set `blob_features.delete_retention_days >= 1`,
# otherwise Defender cannot soft-delete the offending blob.
resource "terraform_data" "defender_soft_delete_guard" {
  count = var.defender_enabled && var.defender_soft_delete_malicious_blobs ? 1 : 0

  lifecycle {
    precondition {
      condition     = var.blob_features.delete_retention_days >= 1
      error_message = "defender_soft_delete_malicious_blobs=true requires blob_features.delete_retention_days >= 1 so that Defender can soft-delete malicious blobs."
    }
  }
}

