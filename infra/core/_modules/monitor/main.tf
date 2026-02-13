locals {
  project = "${var.prefix}-${var.env_short}"

  action_group_selfcare_dev_name = "selcdev"
  action_group_selfcare_uat_name = "selcuat"

  alert_functions_exceptions_name       = "functions-exception"
  alert_functions_exceptions_role_names = ["selc-${var.env_short}-onboarding-fn"]

  test_urls_map = {
    "apigw-selfcare" = {
      host                              = trimsuffix(var.dns_a_api_fqdn, "."),
      path                              = "/external/status",
      frequency                         = 300
      expected_http_status              = 200
      ssl_cert_remaining_lifetime_check = 7
      opsgenie                          = true
    },
    "apigw-pnpg-selfcare" = {
      host                              = trimsuffix(var.dns_a_api_pnpg_fqdn, "."),
      path                              = "/external/status",
      frequency                         = 300,
      expected_http_status              = 200,
      ssl_cert_remaining_lifetime_check = 7,
      opsgenie                          = true
    },
    "login-selfcare" = {
      host                              = trimsuffix(var.cdn_fqdn, "."),
      path                              = "/auth/login",
      frequency                         = 900,
      expected_http_status              = 200,
      ssl_cert_remaining_lifetime_check = 7,
      opsgenie                          = true
    },
    "login-pnpg" = {
      host                              = "imprese.notifichedigitali.it",
      path                              = "/auth/login",
      frequency                         = 900,
      expected_http_status              = 200,
      ssl_cert_remaining_lifetime_check = 7,
      opsgenie                          = true
    }
  }

  test_urls_map_internal = {
    "apigw-selfcare" = {
      host                              = trimsuffix(var.dns_a_api_fqdn, "."),
      path                              = "/external/status",
      frequency                         = 900,
      expected_http_status              = 200,
      ssl_cert_remaining_lifetime_check = 7,
      opsgenie                          = false
    },
    "apigw-pnpg-selfcare" = {
      host                              = trimsuffix(var.dns_a_api_pnpg_fqdn, "."),
      path                              = "/external/status",
      frequency                         = 900,
      expected_http_status              = 200,
      ssl_cert_remaining_lifetime_check = 7,
      opsgenie                          = false
    },
    "login-selfcare" = {
      host                              = trimsuffix(var.cdn_fqdn, "."),
      path                              = "/auth/login",
      frequency                         = 900,
      expected_http_status              = 200,
      ssl_cert_remaining_lifetime_check = 7,
      opsgenie                          = false
    }
  }
}

#
# Data sources for Key Vault secrets
#
data "azurerm_key_vault_secret" "alert_error_notification_email" {
  name         = "alert-error-notification-email"
  key_vault_id = var.key_vault_id
}

data "azurerm_key_vault_secret" "alert_error_notification_slack" {
  name         = "alert-error-notification-slack"
  key_vault_id = var.key_vault_id
}

data "azurerm_key_vault_secret" "monitor_notification_opsgenie" {
  name         = "monitor-notification-opsgenie"
  key_vault_id = var.key_vault_id
}

data "azurerm_key_vault_secret" "monitor_notification_slack_email" {
  name         = "monitor-notification-slack-email"
  key_vault_id = var.key_vault_id
}

data "azurerm_key_vault_secret" "monitor_notification_email" {
  name         = "monitor-notification-email"
  key_vault_id = var.key_vault_id
}

# NOTE: LAW, AppInsights, monitor_rg and dashboards_rg are in log_analytics/ module
# to break circular dependency: monitor needs cdn_fqdn, cdn needs log_analytics_workspace_id

#
# Action Groups
#
resource "azurerm_monitor_action_group" "error_action_group" {
  count = var.env_short == "p" ? 1 : 0

  resource_group_name = var.monitor_rg_name
  name                = "${var.prefix}${var.env_short}error"
  short_name          = "${var.prefix}${var.env_short}error"

  email_receiver {
    name                    = "email"
    email_address           = data.azurerm_key_vault_secret.alert_error_notification_email.value
    use_common_alert_schema = true
  }

  email_receiver {
    name                    = "slack"
    email_address           = data.azurerm_key_vault_secret.alert_error_notification_slack.value
    use_common_alert_schema = true
  }

  webhook_receiver {
    name                    = "opsgenie"
    service_uri             = data.azurerm_key_vault_secret.monitor_notification_opsgenie.value
    use_common_alert_schema = true
  }

  tags = var.tags
}

resource "azurerm_monitor_action_group" "selfcare_status_dev" {
  count = var.env_short == "d" ? 1 : 0

  resource_group_name = var.monitor_rg_name
  name                = local.action_group_selfcare_dev_name
  short_name          = local.action_group_selfcare_dev_name

  email_receiver {
    name                    = "email"
    email_address           = var.selfcare_status_dev_email
    use_common_alert_schema = true
  }

  email_receiver {
    name                    = "slack"
    email_address           = var.selfcare_status_dev_slack
    use_common_alert_schema = true
  }

  tags = var.tags
}

resource "azurerm_monitor_action_group" "selfcare_status_uat" {
  count = var.env_short == "u" ? 1 : 0

  resource_group_name = var.monitor_rg_name
  name                = local.action_group_selfcare_uat_name
  short_name          = local.action_group_selfcare_uat_name

  email_receiver {
    name                    = "email"
    email_address           = var.selfcare_status_uat_email
    use_common_alert_schema = true
  }

  email_receiver {
    name                    = "slack"
    email_address           = var.selfcare_status_uat_slack
    use_common_alert_schema = true
  }

  tags = var.tags
}

resource "azurerm_monitor_action_group" "email" {
  name                = "PagoPA"
  resource_group_name = var.monitor_rg_name
  short_name          = "PagoPA"

  email_receiver {
    name                    = "sendtooperations"
    email_address           = data.azurerm_key_vault_secret.monitor_notification_email.value
    use_common_alert_schema = true
  }

  tags = var.tags
}

resource "azurerm_monitor_action_group" "slack" {
  name                = "SlackPagoPA"
  resource_group_name = var.monitor_rg_name
  short_name          = "SlackPagoPA"

  email_receiver {
    name                    = "sendtoslack"
    email_address           = data.azurerm_key_vault_secret.monitor_notification_slack_email.value
    use_common_alert_schema = true
  }

  tags = var.tags
}

#
# Web Tests
#
module "web_test_api" {
  for_each = var.env_short == "p" ? local.test_urls_map : local.test_urls_map_internal

  source = "github.com/pagopa/terraform-azurerm-v4.git//application_insights_web_test_preview?ref=v6.6.0"

  subscription_id                   = var.subscription_id
  name                              = "${each.key}-test"
  location                          = var.monitor_rg_location
  resource_group                    = var.monitor_rg_name
  application_insight_name          = var.application_insights_name
  application_insight_id            = var.application_insights_id
  request_url                       = "https://${each.value.host}${each.value.path}"
  ssl_cert_remaining_lifetime_check = each.value.ssl_cert_remaining_lifetime_check
  expected_http_status              = each.value.expected_http_status
  frequency                         = each.value.frequency
  alert_description                 = "Web availability check alert triggered when it fails. Runbook: https://pagopa.atlassian.net/wiki/spaces/SCP/pages/823722319/Web+Availability+Test+-+TLS+Probe+Check"

  actions = var.env_short == "p" && each.value.opsgenie ? [
    {
      action_group_id    = azurerm_monitor_action_group.error_action_group[0].id
      webhook_properties = null
    }
    ] : [
    {
      action_group_id    = azurerm_monitor_action_group.slack.id
      webhook_properties = null
    },
    {
      action_group_id    = azurerm_monitor_action_group.email.id
      webhook_properties = null
    }
  ]
}

#
# Dashboard
#
resource "azurerm_portal_dashboard" "monitoring-dashboard" {
  name                = "${local.project}-monitoring-dashboard"
  resource_group_name = var.monitor_rg_name
  location            = var.monitor_rg_location
  tags                = var.tags

  dashboard_properties = templatefile("${path.module}/../dashboards/monitoring.json.tpl",
    {
      subscription_id = var.subscription_id
      prefix          = local.project
  })
}

#
# Metric Alerts
#
resource "azurerm_monitor_metric_alert" "functions_exceptions" {
  count = var.env_short == "d" ? 0 : 1

  name                = local.alert_functions_exceptions_name
  resource_group_name = var.monitor_rg_name
  scopes              = [var.application_insights_id]
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
    action_group_id = azurerm_monitor_action_group.slack.id
  }
}
