locals {
  namespace = var.sku == "Standard" ? {
    id   = azurerm_servicebus_namespace.standard[0].id
    name = azurerm_servicebus_namespace.standard[0].name
    } : {
    id   = module.premium_namespace[0].id
    name = module.premium_namespace[0].name
  }
}

# radar: assess -- explicitly approved for webhook delivery reliability.
resource "azurerm_servicebus_namespace" "standard" {
  count = var.sku == "Standard" ? 1 : 0

  name                = "${var.environment.prefix}-${var.environment.env_short}-${var.environment.location_short}-${var.environment.app_name}-sbns-${var.environment.instance_number}"
  location            = var.environment.location
  resource_group_name = var.resource_group_name
  sku                 = var.sku

  local_auth_enabled = false

  # Standard uses a public endpoint restricted to the NAT IP; Premium disables
  # public access and is reached through the private endpoint below.
  public_network_access_enabled = var.sku == "Standard"

  dynamic "network_rule_set" {
    for_each = var.sku == "Standard" ? [true] : []

    content {
      default_action                = "Deny"
      public_network_access_enabled = true
      trusted_services_allowed      = false
      ip_rules                      = [data.azurerm_public_ip.outbound[0].ip_address]
    }
  }

  tags = var.tags
}

# The DX module owns the Premium namespace, private endpoint, private DNS
# registration, diagnostics, and autoscaling configuration.
module "premium_namespace" {
  count = var.sku == "Premium" ? 1 : 0

  source  = "pagopa-dx/azure-service-bus-namespace/azurerm"
  version = "~> 1.0"

  environment = {
    prefix          = var.environment.prefix
    env_short       = var.environment.env_short
    location        = var.environment.location
    domain          = var.environment.domain
    app_name        = var.environment.app_name
    instance_number = var.environment.instance_number
  }

  resource_group_name                  = var.resource_group_name
  subnet_pep_id                        = data.azurerm_subnet.private_endpoints[0].id
  private_dns_zone_resource_group_name = var.private_dns_zone_resource_group_name

  # The DX default use case provisions a Premium namespace with private access.
  use_case = "default"
  diagnostic_settings = {
    enabled                    = true
    log_analytics_workspace_id = data.azurerm_log_analytics_workspace.monitoring.id
  }

  tags = var.tags
}

resource "azurerm_servicebus_queue" "this" {
  name         = var.queue_name
  namespace_id = local.namespace.id

  lock_duration                        = "PT1M"
  max_delivery_count                   = 10
  default_message_ttl                  = "P14D"
  dead_lettering_on_message_expiration = true
}

resource "azurerm_monitor_diagnostic_setting" "this" {
  count = var.sku == "Standard" ? 1 : 0

  name                       = "${local.namespace.name}-diagnostics"
  target_resource_id         = local.namespace.id
  log_analytics_workspace_id = data.azurerm_log_analytics_workspace.monitoring.id

  enabled_log {
    category_group = "allLogs"
  }

  enabled_metric {
    category = "AllMetrics"
  }
}
