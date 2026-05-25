variable "image_tag" {
  type        = string
  description = "Image tag"
  default     = "latest"
}

variable "blob_storage_account_name" {
  type        = string
  description = "Name of the Azure Storage Account used for open-data CSV snapshots (same account as web-storage-connection-string)"
}

variable "blob_storage_account_rg" {
  type        = string
  description = "Resource group of the Azure Storage Account used for open-data CSV snapshots"
}
