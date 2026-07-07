variable "app_name" {
  type        = string
  default     = "usrattach"

  validation {
    condition     = length(replace(var.app_name, "-", "")) <= 9
    error_message = "app_name (hyphens stripped) must be at most 9 characters to keep the generated storage account name within the 24-char Azure limit."
  }
}

variable "container_name" {
  type        = string
  default     = null
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

variable "cidr_subnet_user_attachments_storage" {
  type        = list(string)
  description = "User attachments storage subnet address space (dedicated /24 recommended)."
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
  description = "The name of the resource group holding private DNS zone to use for private endpoints. Default is Virtual Network resource group"
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

# ---- Lifecycle management policy ----------------------------------------------
# NOTE: user attachments will typically be short-lived (soft-delete after onboarding
# closure / rejection). Defaults are conservative and can be overridden per env.

variable "base_blob_tier_to_cool_after_days_since_modification_greater_than" {
  type    = number
  default = 30
}

variable "base_blob_tier_to_cold_after_days_since_creation_greater_than" {
  type    = number
  default = 60
}

variable "base_delete_after_days_since_creation_greater_than" {
  type        = number
  description = "Days after which blobs matching the lifecycle prefix filter are deleted. Overridden per environment."
  default     = 90
}

variable "lifecycle_prefix_match" {
  type        = list(string)
  default     = ["parties/deleted"]
}

variable "snapshot_change_tier_to_cool_after_days_since_creation" {
  type    = number
  default = 30
}

variable "snapshot_delete_after_days_since_creation_greater_than" {
  type    = number
  default = 90
}

variable "version_change_tier_to_cool_after_days_since_creation" {
  type    = number
  default = 30
}

variable "version_delete_after_days_since_creation" {
  type    = number
  default = 90
}

# ---- Defender for Storage -----------------------------------------------------

variable "defender_enabled" {
  type        = bool
  description = "Enable Microsoft Defender for Storage on this storage account (recommended for user-uploaded content). See security team recommendation."
  default     = true
}

variable "defender_malware_scanning_enabled" {
  type        = bool
  description = "Enable on-upload malware scanning by Defender for Storage."
  default     = true
}

variable "defender_malware_scanning_cap_gb_per_month" {
  type        = number
  description = "Monthly cap (GB) for malware scanning. Set to -1 for unlimited."
  default     = 5000
}

variable "defender_sensitive_data_discovery_enabled" {
  type        = bool
  description = "Enable Sensitive Data Discovery by Defender for Storage."
  default     = false
}

variable "defender_soft_delete_malicious_blobs" {
  type        = bool
  description = "When true, malicious blobs detected by malware scanning are soft-deleted (active block). Requires blob_features.delete_retention_days >= 1."
  default     = true
}

