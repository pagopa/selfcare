variable "prefix" {
  type    = string
  default = "selc"
}

variable "env_short" {
  type = string
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "tags" {
  type    = map(any)
  default = {}
}

variable "private_endpoint_network_policies" {
  type    = string
  default = "Enabled"
}

# From network module
variable "rg_vnet_name" {
  type = string
}

variable "vnet_id" {
  type = string
}

variable "vnet_name" {
  type = string
}

# From key_vault module
variable "key_vault_id" {
  type = string
}

# From monitor module
variable "action_group_error_id" {
  type    = string
  default = null
}

variable "action_group_slack_id" {
  type = string
}

variable "action_group_email_id" {
  type = string
}

# From dns_private module
variable "privatelink_servicebus_windows_net_ids" {
  type        = list(string)
  description = "Private DNS zone IDs for servicebus"
}

variable "privatelink_servicebus_windows_net_names" {
  type        = list(string)
  description = "Private DNS zone names for servicebus"
}

# EventHub configuration
variable "cidr_subnet_eventhub" {
  type        = list(string)
  description = "EventHub subnet address space"
}

variable "eventhub_auto_inflate_enabled" {
  type    = bool
  default = false
}

variable "eventhub_sku_name" {
  type    = string
  default = "Basic"
}

variable "eventhub_capacity" {
  type    = number
  default = null
}

variable "eventhub_maximum_throughput_units" {
  type    = number
  default = null
}

variable "eventhubs" {
  description = "A list of event hub topics to add to namespace."
  type = list(object({
    name              = string
    partitions        = number
    message_retention = number
    consumers         = list(string)
    keys = list(object({
      name   = string
      listen = bool
      send   = bool
      manage = bool
    }))
    iam_roles = optional(map(string), {})
  }))
  default = []
}

variable "eventhub_ip_rules" {
  type = list(object({
    ip_mask = string
    action  = string
  }))
  default = []
}

variable "eventhub_alerts_enabled" {
  type    = bool
  default = false
}

variable "eventhub_metric_alerts" {
  default = {}
  type = map(object({
    aggregation = string
    metric_name = string
    description = string
    operator    = string
    threshold   = number
    frequency   = string
    window_size = string
    dimension = list(object({
      name     = string
      operator = string
      values   = list(string)
    }))
  }))
}
