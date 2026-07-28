variable "app_name" {
  type        = string
  description = "App name"
}

variable "blob_features" {
  type = object({
    restore_policy_days   = optional(number, 0)
    delete_retention_days = optional(number, 0)
    last_access_time      = optional(bool, false)
    versioning            = optional(bool, false)
    change_feed = optional(object({
      enabled           = optional(bool, false)
      retention_in_days = optional(number, 0)
    }), { enabled = false })
    immutability_policy = optional(object({
      enabled                       = optional(bool, false)
      allow_protected_append_writes = optional(bool, false)
      period_since_creation_in_days = optional(number, 730)
    }), { enabled = false })
  })
  description = "Advanced blob features like versioning, change feed, immutability, and retention policies."
  default = {
    restore_policy_days   = 0
    delete_retention_days = 0
    last_access_time      = false
    versioning            = false
    change_feed           = { enabled = false, retention_in_days = 0 }
    immutability_policy   = { enabled = false }
  }

  validation {
    condition     = (var.blob_features.immutability_policy.enabled == true && var.blob_features.restore_policy_days == 0) || var.blob_features.immutability_policy.enabled == false
    error_message = "Immutability policy doesn't support Point-in-Time restore."
  }

  validation {
    condition     = var.blob_features.delete_retention_days == 0 || (var.blob_features.delete_retention_days >= 1 && var.blob_features.delete_retention_days <= 365)
    error_message = "Delete retention days must be 0 to disable the policy or between 1 and 365."
  }

  validation {
    condition     = var.blob_features.restore_policy_days == 0 || (var.blob_features.restore_policy_days >= 1 && var.blob_features.restore_policy_days <= 365)
    error_message = "Restore policy days must be 0 to disable the policy or between 1 and 365."
  }
}

variable "cidr_subnet_contract_storage" {
  type        = list(string)
  description = "Documents storage address space."
}

variable "domain" {
  type        = string
  description = "Domain"
}

variable "env_short" {
  description = "Environment short name"
  type        = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}

variable "instance_number" {
  type        = string
  description = "The istance number to create"
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "key_vault_resource_group_name" {
  type        = string
  description = "Name of Key Vault resource group"
}

variable "key_vault_name" {
  type        = string
  description = "Name of Key Vault"
}

variable "prefix" {
  description = "Domain prefix"
  type        = string
  default     = "selc"
  validation {
    condition = (
      length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "private_dns_zone_resource_group_name" {
  type        = string
  description = "he name of the resource group holding private DNS zone to use for private endpoints. Default is Virtual Network resource group"
}

variable "project" {
  type        = string
  description = "Selfcare prefix and short environment"
}

variable "virtual_network_name" {
  type        = string
  description = "Name of the resource where resources will be created"
}

variable "suffix_increment" {
  type        = string
  description = "Suffix increment Container App Environment name"
  default     = ""
}

variable "resource_group_name" {
  type        = string
  description = "Resource group"
}

variable "tags" {
  type = map(any)
}

variable "base_blob_tier_to_cool_after_days_since_modification_greater_than" {
  type = number
}

variable "base_blob_tier_to_cold_after_days_since_creation_greater_than" {
  type = number
}

variable "base_delete_after_days_since_creation_greater_than" {
  type = number
}

variable "snapshot_change_tier_to_cool_after_days_since_creation" {
  type = number
}

variable "snapshot_delete_after_days_since_creation_greater_than" {
  type = number
}

variable "version_change_tier_to_cool_after_days_since_creation" {
  type = number
}

variable "version_delete_after_days_since_creation" {
  type = number
}

# -----------------------------------------------------------------------------
# Generic knobs — allow the caller to fully decide semantics (subnet name,
# key vault secret name, lifecycle filter). Defaults preserve backward
# compatibility for pre-existing callers (module "storage_documents").
# -----------------------------------------------------------------------------

variable "naming_config" {
  type        = string
  description = "Logical name used to build the storage subnet name (e.g. \"documents\", \"user-attachments\")."
  default     = "documents"
}

variable "kv_secret_name" {
  type        = string
  description = "Name of the Key Vault secret that will hold the primary connection string."
  default     = "documents-storage-connection-string"
}

variable "lifecycle_prefix_match" {
  type        = list(string)
  description = "Blob path prefixes the lifecycle management policy applies to."
  default     = ["parties/deleted"]
}

# -----------------------------------------------------------------------------
# Microsoft Defender for Storage (per-account). Disabled by default so existing
# callers keep working; new callers can opt in.
#
# NOTE: as of pagopa-dx/azure-storage-account v1.x, these values are ACCEPTED
# but NOT enforced at the ARM level — the upstream module does not expose the
# needed inputs. See storage_account.tf for the full rationale. When the new
# pagopa-dx version is released with the corresponding inputs, storage_account.tf
# will start wiring these caller-facing variables to the upstream module and
# they will take effect without any change on callers' side.
# -----------------------------------------------------------------------------
variable "defender_enabled" {
  type        = bool
  description = "Enable Microsoft Defender for Storage on this storage account."
  default     = false
}

variable "defender_malware_scanning_enabled" {
  type        = bool
  description = "Enable on-upload malware scanning by Defender for Storage. Requires defender_enabled = true."
  default     = false
}

variable "defender_malware_scanning_cap_gb_per_month" {
  type        = number
  description = "Monthly cap (GB) for malware scanning. Set to -1 for unlimited. Only used when malware scanning is enabled."
  default     = null
}

variable "defender_sensitive_data_discovery_enabled" {
  type        = bool
  description = "Enable Sensitive Data Discovery by Defender for Storage. Requires defender_enabled = true."
  default     = false
}

variable "defender_soft_delete_malicious_blobs" {
  type        = bool
  description = "When true, malicious blobs detected by malware scanning are soft-deleted. Requires blob_features.delete_retention_days >= 1."
  default     = false
}
