###############################################################################
# Monitoring
###############################################################################

data "azurerm_application_insights" "application_insights" {
  name                = "${local.prefix}-${local.env_short}-appinsights"
  resource_group_name = data.azurerm_resource_group.monitor_rg.name
}

###############################################################################
# Action
###############################################################################

data "azurerm_monitor_action_group" "slack" {
  resource_group_name = data.azurerm_resource_group.monitor_rg.name
  name                = local.monitor_action_group_slack_name
}

data "azurerm_monitor_action_group" "email" {
  resource_group_name = data.azurerm_resource_group.monitor_rg.name
  name                = local.monitor_action_group_email_name
}

data "azurerm_key_vault_secret" "alert_pnpg_http_status_slack" {
  name         = "alert-pnpg-http-status-slack"
  key_vault_id = module.key_vault.key_vault_id
}


resource "azurerm_monitor_action_group" "http_status" {
  count = local.env_short == "d" ? 0 : 1

  resource_group_name = data.azurerm_resource_group.monitor_rg.name
  name                = "HttpStatus-${local.env_short}"
  short_name          = "HttpStatus-${local.env_short}"

  email_receiver {
    name                    = "slack"
    email_address           = data.azurerm_key_vault_secret.alert_pnpg_http_status_slack.value
    use_common_alert_schema = true
  }

  tags = local.tags
}

resource "azurerm_monitor_metric_alert" "http_error_5xx" {
  count = local.env_short == "d" ? 0 : 1

  name                = "http-error-5xx"
  resource_group_name = data.azurerm_resource_group.monitor_rg.name
  scopes              = [data.azurerm_application_insights.application_insights.id]
  description         = "Action will be triggered when Request with http 5xx status happens."
  auto_mitigate       = false

  criteria {
    metric_namespace = "Microsoft.Insights/Components"
    metric_name      = "requests/failed"
    aggregation      = "Count"
    operator         = "GreaterThan"
    threshold        = 0

    dimension {
      name     = "request/resultCode"
      operator = "Include"
      values   = ["500", "501", "502"]
    }
  }

  action {
    action_group_id = azurerm_monitor_action_group.http_status[0].id
  }
}

resource "azurerm_monitor_metric_alert" "unhealthy_error_503" {
  count = local.env_short == "d" ? 0 : 1

  name                = "unhealthy-error-503"
  resource_group_name = data.azurerm_resource_group.monitor_rg.name
  scopes              = [data.azurerm_application_insights.application_insights.id]
  description         = "Action will be triggered when a resource fails health check returning 503 error."
  auto_mitigate       = false

  criteria {
    metric_namespace = "Microsoft.Insights/Components"
    metric_name      = "requests/failed"
    aggregation      = "Count"
    operator         = "GreaterThan"
    threshold        = 5

    dimension {
      name     = "request/resultCode"
      operator = "Include"
      values   = ["503"]
    }
  }

  action {
    action_group_id = azurerm_monitor_action_group.http_status[0].id
  }
}

resource "azurerm_monitor_metric_alert" "functions_exceptions" {
  count = local.env_short == "d" ? 0 : 1

  name                = local.alert_functions_exceptions_name
  resource_group_name = data.azurerm_resource_group.monitor_rg.name
  scopes              = [data.azurerm_application_insights.application_insights.id]
  description         = "Action will be triggered when Functions throw some exceptions."
  auto_mitigate       = false

  criteria {
    metric_namespace = "Microsoft.Insights/Components"
    metric_name      = "exceptions/count"
    aggregation      = "Count"
    operator         = "GreaterThan"
    threshold        = 2

    dimension {
      name     = "cloud/roleName"
      operator = "Include"
      values   = local.alert_functions_exceptions_role_names
    }
  }

  action {
    action_group_id = azurerm_monitor_action_group.http_status[0].id
  }
}

###############################################################################
# Log Alert: HTTP 400 rate per single API (operation_Name)
# Fires when a single API returns more than the threshold of HTTP 400
# within the evaluation window. Evaluated per operation_Name via dimension split.
###############################################################################
resource "azurerm_monitor_scheduled_query_rules_alert_v2" "api_400_rate" {
  count = local.env_short == "d" ? 0 : 1

  name                = "${local.prefix}-${local.env_short}-api-400-rate"
  resource_group_name = data.azurerm_resource_group.monitor_rg.name
  location            = data.azurerm_resource_group.monitor_rg.location

  description             = "Triggered when a single API (operation_Name) returns more than ${local.api_400_alert_threshold} HTTP 400 within ${local.api_400_alert_window_frequency}."
  severity                = local.api_400_alert_severity
  enabled                 = true
  auto_mitigation_enabled = false

  scopes               = [data.azurerm_application_insights.application_insights.id]
  evaluation_frequency = local.api_400_alert_window_frequency
  window_duration      = local.api_400_alert_window_frequency

  criteria {
    query = <<-KQL
      requests
      | where resultCode == "400"
      | summarize Count = count() by operation_Name
    KQL

    time_aggregation_method = "Total"
    metric_measure_column   = "Count"
    operator                = "GreaterThan"
    threshold               = local.api_400_alert_threshold

    dimension {
      name     = "operation_Name"
      operator = "Include"
      values   = ["*"]
    }

    failing_periods {
      number_of_evaluation_periods             = 1
      minimum_failing_periods_to_trigger_alert = 1
    }
  }

  action {
    action_groups = [azurerm_monitor_action_group.http_status[0].id]
  }

  tags = local.tags
}


###############################################################################
# Network
###############################################################################
data "azurerm_dns_zone" "public" {
  name                = "${local.dns_zone_prefix}.${local.external_domain}"
  resource_group_name = "${local.prefix}-${local.env_short}-vnet-rg"
}

data "azurerm_private_dns_zone" "internal" {
  name                = "internal.${local.dns_zone_prefix}.${local.external_domain}"
  resource_group_name = data.azurerm_dns_zone.public.resource_group_name
}

resource "azurerm_private_dns_a_record" "ingress" {
  name                = "${local.env}01.${local.app_domain}"
  zone_name           = data.azurerm_private_dns_zone.internal.name
  resource_group_name = data.azurerm_private_dns_zone.internal.resource_group_name
  ttl                 = 3600
  records             = [local.ingress_load_balancer_ip]
}


###############################################################################
# Secrets
###############################################################################

# #tfsec:ignore:azure-keyvault-ensure-secret-expiry
resource "azurerm_key_vault_secret" "appinsights-instrumentation-key" {
  key_vault_id = module.key_vault.key_vault_id
  name         = "appinsights-instrumentation-key"
  value        = data.azurerm_application_insights.application_insights.connection_string
  content_type = "text/plain"
}

###############################################################################
# NAT
###############################################################################

resource "azurerm_resource_group" "nat_rg" {
  name     = "${local.prefix}-${local.env_short}-${local.location_short}-${local.app_domain}-nat-rg"
  location = local.location
  tags     = local.tags
}

resource "azurerm_public_ip" "pip_outbound" {
  name                = "${local.prefix}-${local.env_short}-${local.location_short}-${local.app_domain}-pip-outbound"
  resource_group_name = azurerm_resource_group.nat_rg.name
  location            = azurerm_resource_group.nat_rg.location
  sku                 = "Standard"
  sku_tier            = "Regional"
  allocation_method   = "Static"

  zones = [
    "1",
    "2",
    "3",
  ]

  tags = local.tags
}

resource "azurerm_public_ip" "functions_pip_outbound" {
  name                = "${local.app_name_fn}-functions-pip-outbound"
  resource_group_name = azurerm_resource_group.nat_rg.name
  location            = azurerm_resource_group.nat_rg.location
  sku                 = "Standard"
  sku_tier            = "Regional"
  allocation_method   = "Static"

  tags = local.tags
}

resource "azurerm_nat_gateway" "nat_gateway" {
  name                    = "${local.prefix}-${local.env_short}-${local.location_short}-${local.app_domain}-nat_gw"
  resource_group_name     = azurerm_resource_group.nat_rg.name
  location                = azurerm_resource_group.nat_rg.location
  sku_name                = "Standard"
  idle_timeout_in_minutes = 10
}


###############################################################################
# assets
###############################################################################

# module "assets" {
#   source = "../_modules/assets"

#   env        = local.env
#   app_domain = local.app_domain
#   # CDN
#   checkout_cdn_name                       = module.cdn.storage_name
#   checkout_endpoint_name                  = module.cdn.name
#   checkout_cdn_storage_primary_access_key = module.cdn.storage_primary_access_key
#   checkout_fe_rg_name                     = module.cdn.checkout_fe_rg_name
# }
