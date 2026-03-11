variable "name" {
  type        = string
  description = "The name of the MongoDB database."
}

variable "resource_group_name" {
  type        = string
  description = "The name of the resource group."
}

variable "account_name" {
  type        = string
  description = "The name of the CosmosDB account."
}

variable "throughput" {
  type        = number
  description = "The throughput of the MongoDB database (RU/s)."
  default     = 1000
}
