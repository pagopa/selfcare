locals {
  project = "${var.prefix}-${var.env_short}"

  # Total rewrite rules count for calculating subsequent rule orders
  total_rewrite_rules = 1 + length(var.spa) # defaultApplication + SPA rules

  # delivery_rule_rewrites: defaultApplication + SPA rewrites
  # New format uses typed condition lists instead of generic conditions with condition_type
  app_delivery_rules = concat(
    [{
      name  = "defaultApplication"
      order = 3

      url_path_conditions = [{
        operator         = "Equal"
        match_values     = ["/"]
        negate_condition = false
        transforms       = []
      }]

      url_rewrite_actions = [{
        source_pattern          = "/"
        destination             = "/dashboard/index.html"
        preserve_unmatched_path = "false"
      }]
    }],
    [for i, spa in var.spa : {
      name  = replace(format("SPA-%s", spa), "-", "")
      order = i + 4

      url_path_conditions = [{
        operator         = "BeginsWith"
        match_values     = [format("/%s/", spa)]
        negate_condition = false
        transforms       = []
      }]

      url_file_extension_conditions = [{
        operator         = "LessThanOrEqual"
        match_values     = ["0"]
        negate_condition = false
        transforms       = []
      }]

      url_rewrite_actions = [{
        source_pattern          = format("/%s/", spa)
        destination             = format("/%s/index.html", spa)
        preserve_unmatched_path = "false"
      }]
    }]
  )

  cors = {
    paths = ["/assets/"]
  }
}

###############################################################################
# Resource Group
###############################################################################
resource "azurerm_resource_group" "checkout_fe_rg" {
  name     = format("%s-checkout-fe-rg", local.project)
  location = var.location
  tags     = var.tags
}

###############################################################################
# CDN Front Door (migrated from CDN Classic)
###############################################################################
module "checkout_cdn" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//cdn_frontdoor?ref=v9.6.1"

  cdn_prefix_name     = "${local.project}-checkout"
  resource_group_name = azurerm_resource_group.checkout_fe_rg.name
  location            = var.location
  tags                = var.tags

  # Storage
  storage_account_replication_type   = var.storage_account_replication_type
  storage_account_index_document     = "index.html"
  storage_account_error_404_document = "error.html"

  # Diagnostics
  log_analytics_workspace_id = var.log_analytics_workspace_id

  # Routing
  https_rewrite_enabled         = true
  querystring_caching_behaviour = "IgnoreQueryString"

  # Key Vault (for apex custom domain certificate)
  keyvault_id = var.key_vault_id
  tenant_id   = var.tenant_id

  # Custom domain
  custom_domains = [{
    domain_name             = "${var.dns_zone_prefix}.${var.external_domain}"
    dns_name                = "${var.dns_zone_prefix}.${var.external_domain}"
    dns_resource_group_name = var.rg_vnet_name
    ttl                     = 3600
    enable_dns_records      = true
  }]

  # Global delivery rules (header mutations applied to every request)
  # Split into two rules because Azure Front Door allows max 5 actions per rule
  global_delivery_rules = [
    {
      order = 1

      modify_response_header_actions = [
        {
          action = "Overwrite"
          name   = "Strict-Transport-Security"
          value  = "max-age=31536000"
        },
        {
          action = "Overwrite"
          name   = "Content-Security-Policy-Report-Only"
          value = format("default-src 'self'; object-src 'none'; connect-src 'self' https://api.%s.%s/ https://api-eu.mixpanel.com/track/; "
          , var.dns_zone_prefix, var.external_domain)
        },
        {
          action = "Append"
          name   = "Content-Security-Policy-Report-Only"
          value  = "script-src 'self'; style-src 'self' 'unsafe-inline' https://selfcare.pagopa.it/assets/font/selfhostedfonts.css; worker-src 'none'; font-src 'self' https://selfcare.pagopa.it/assets/font/; "
        },
        {
          action = "Append"
          name   = "Content-Security-Policy-Report-Only"
          value  = format("img-src 'self' https://assets.cdn.io.italia.it https://%s data:; ", module.checkout_cdn.storage_primary_web_host)
        },
        {
          action = "Append"
          name   = "X-Content-Type-Options"
          value  = "nosniff"
        }
      ]
    },
    {
      order = 2

      modify_response_header_actions = [
        {
          action = "Append"
          name   = "Content-Security-Policy"
          value  = format("frame-ancestors 'none'; object-src 'none'; frame-src 'self' *.%s.%s;", var.dns_zone_prefix, var.external_domain)
        }
      ]
    }
  ]

  # Application rewrite rules (SPA routing)
  delivery_rule_rewrites = local.app_delivery_rules

  # Custom delivery rules (robots, cache, CORS)
  delivery_custom_rules = [
    {
      name  = "robotsNoIndex"
      order = 20 + local.total_rewrite_rules + 1

      url_path_conditions = [{
        operator         = "Equal"
        match_values     = length(var.robots_indexed_paths) > 0 ? var.robots_indexed_paths : ["dummy"]
        negate_condition = true
        transforms       = []
      }]

      modify_response_header_actions = [{
        action = "Overwrite"
        name   = "X-Robots-Tag"
        value  = "noindex, nofollow"
      }]
    },
    {
      name  = "microcomponentsNoCache"
      order = 20 + local.total_rewrite_rules + 2

      url_file_name_conditions = [{
        operator         = "Equal"
        match_values     = ["remoteEntry.js"]
        negate_condition = false
        transforms       = []
      }]

      modify_response_header_actions = [{
        action = "Overwrite"
        name   = "Cache-Control"
        value  = "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0"
      }]
    },
    {
      name  = "cors"
      order = 20 + local.total_rewrite_rules + 3

      url_path_conditions = [{
        operator         = "BeginsWith"
        match_values     = local.cors.paths
        negate_condition = false
        transforms       = []
      }]

      modify_response_header_actions = [{
        action = "Overwrite"
        name   = "Access-Control-Allow-Origin"
        value  = "*"
      }]
    }
  ]
}

###############################################################################
# Key Vault Secrets
###############################################################################

#tfsec:ignore:azure-keyvault-ensure-secret-expiry
resource "azurerm_key_vault_secret" "selc_web_storage_access_key" {
  name         = "web-storage-access-key"
  value        = module.checkout_cdn.storage_primary_access_key
  content_type = "text/plain"
  key_vault_id = var.key_vault_id
}

#tfsec:ignore:azure-keyvault-ensure-secret-expiry
resource "azurerm_key_vault_secret" "selc_web_storage_connection_string" {
  name         = "web-storage-connection-string"
  value        = module.checkout_cdn.storage_primary_connection_string
  content_type = "text/plain"
  key_vault_id = var.key_vault_id
}

#tfsec:ignore:azure-keyvault-ensure-secret-expiry
resource "azurerm_key_vault_secret" "selc_web_storage_blob_connection_string" {
  name         = "web-storage-blob-connection-string"
  value        = module.checkout_cdn.storage_primary_blob_connection_string
  content_type = "text/plain"
  key_vault_id = var.key_vault_id
}
