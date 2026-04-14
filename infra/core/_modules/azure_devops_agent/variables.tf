variable "project" {
  type    = string
  default = "selc"
}

variable "enable_azdoa" {
  type        = bool
  description = "Enable Azure DevOps agent."
}

variable "tags" {
  type = map(any)
  default = {
    CreatedBy = "Terraform"
  }
}

variable "location" {
  type    = string
  default = "westeurope"
}


variable "enable_iac_pipeline" {
  type        = bool
  description = "If true create the key vault policy to allow used by azure devops iac pipelines."
  default     = false
}

variable "enable_app_projects_pipeline" {
  type        = bool
  description = "If true create the key vault policy to allow used by azure devops app projects pipelines."
  default     = false
}

variable "cidr_subnet_azdoa" {
  type        = list(string)
  description = "Azure DevOps agent network address space."
}

variable "rg_vnet_name" {
  type        = string
  description = "Resource group name for the VNet (for DNS zone)"
}

variable "vnet_name" {
  type        = string
  description = "VNet name for the VNet (for DNS zone)"
}


variable "private_endpoint_network_policies" {
  type        = string
  description = "Private endpoint network policies"
  default     = "Enabled"
}


variable "subscription_id" {
  type        = string
  description = "Azure subscription ID"
}

variable "env_short" {
  type = string
  validation {
    condition = (
      length(var.env_short) <= 1
    )
    error_message = "Max length is 1 chars."
  }
}

variable "azdo_agent_vm_sku" {
  type        = string
  description = "sku of the azdo agent vm"
  default     = "Standard_B1s"
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault ID"
}

variable "tenant_id" {
  type        = string
  description = "Azure AD tenant ID for Front Door managed identity access to Key Vault"
}

variable "iac_principal_object_id" {
  type        = string
  description = "Service principal for IAC pipelines"
}

variable "app_projects_principal_object_id" {
  type        = string
  description = "Service principal for App Projects pipelines"
}