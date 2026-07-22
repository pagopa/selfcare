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

  # -----------------------------------------------------------------------------
  # Microsoft Defender for Storage — WAITING FOR UPSTREAM SUPPORT.
  #
  # The pagopa-dx module v1.x creates an
  # `azurerm_security_center_storage_defender.this` internally but does NOT
  # expose the malware scanning / subscription override attributes as inputs.
  # As agreed on review, we asked pagopa-dx to release a new version exposing
  # these variables. When it lands, UNCOMMENT the lines below and bump
  # `version = "~> X.Y"` above accordingly. The final upstream variable names
  # may differ slightly — check the pagopa-dx CHANGELOG / variables.tf and
  # adapt the mapping (this module already exposes the caller-facing
  # `defender_*` inputs, so callers won't need any change).
  #
  # defender_malware_scanning_on_upload_enabled          = var.defender_malware_scanning_enabled
  # defender_malware_scanning_on_upload_cap_gb_per_month = coalesce(var.defender_malware_scanning_cap_gb_per_month, -1)
  # defender_override_subscription_settings_enabled      = var.defender_enabled
  # defender_sensitive_data_discovery_enabled            = var.defender_sensitive_data_discovery_enabled
  # -----------------------------------------------------------------------------
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

resource "azurerm_key_vault_secret" "selc_documents_storage_connection_string" {
  name         = var.kv_secret_name
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

################################################################################
# Microsoft Defender for Storage — validation guardrail.
#
# The upstream `pagopa-dx/azure-storage-account` module v1.x already creates an
# `azurerm_security_center_storage_defender.this` internally with hardcoded
# defaults: `malware_scanning_on_upload_enabled = false`,
# `override_subscription_settings_enabled = false`, etc. It does not expose any
# input to configure these attributes.
################################################################################

resource "terraform_data" "defender_soft_delete_guard" {
  count = var.defender_enabled && var.defender_soft_delete_malicious_blobs ? 1 : 0

  lifecycle {
    precondition {
      condition     = var.blob_features.delete_retention_days >= 1
      error_message = "defender_soft_delete_malicious_blobs=true requires blob_features.delete_retention_days >= 1 so that Defender can soft-delete malicious blobs."
    }
  }
}

