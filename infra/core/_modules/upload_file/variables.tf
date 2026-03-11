variable "file_path" {
  type        = string
  description = "File path"
}

variable "container" {
  type        = string
  description = "Storage container name"
}

variable "primary_connection_string" {
  type        = string
  sensitive   = true
  description = "Storage account primary connection string"
}
