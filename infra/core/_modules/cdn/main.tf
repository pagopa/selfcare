locals {

  environment = {
    prefix          = var.prefix
    env_short       = var.env_short
    location        = var.location
    domain          = var.domain
    app_name        = "checkout"
    instance_number = tonumber(var.instance_number)
  }

  basename = "${var.prefix}${var.env_short}${var.location_short}${var.domain}"

  spa = [
    for i, spa in var.spa :
    {
      name  = replace(format("SPA-%s", spa), "-", "")
      order = i + 3
      conditions = [
        {
          condition_type   = "url_path_condition"
          operator         = "BeginsWith"
          match_values     = ["/${spa}/"]
          negate_condition = false
          transforms       = null
        },
        {
          condition_type   = "url_file_extension_condition"
          operator         = "LessThanOrEqual"
          match_values     = ["0"]
          negate_condition = false
          transforms       = null
        },
      ]
      url_rewrite_action = {
        source_pattern          = "/${spa}/"
        destination             = "/${spa}/index.html"
        preserve_unmatched_path = false
      }
    }
  ]

  cors = {
    paths = ["/assets/"]
  }
}

#
# Resource Group
#
resource "azurerm_resource_group" "checkout_fe_rg" {
  name     = format("%s-checkout-fe-rg", var.project)
  location = var.location
  tags     = var.tags
}

resource "azurerm_subnet" "cdn_snet" {
  count = var.create_snet ? 1 : 0

  name                 = "${var.project}-${local.environment.app_name}-snet"
  virtual_network_name = var.vnet_name
  resource_group_name  = var.rg_vnet_name
  address_prefixes     = var.cidr_subnet_cdn

  private_endpoint_network_policies = "Enabled"

  service_endpoints = [
    "Microsoft.Storage",
  ]
}

data "azurerm_subnet" "cdn_snet" {
  count = var.create_snet ? 0 : 1

  name                 = "${local.environment.prefix}-${local.environment.env_short}-${local.environment.app_name}-snet"
  virtual_network_name = var.vnet_name
  resource_group_name  = var.rg_vnet_name
}




###############################################################################
# Storage Account (static website) — pagopa-dx module
###############################################################################
module "cdn_storage_account" {
  source  = "pagopa-dx/azure-storage-account/azurerm"
  version = "~> 2.1"

  environment = local.environment

  resource_group_name                 = azurerm_resource_group.checkout_fe_rg.name
  use_case                            = var.storage_use_case
  subnet_pep_id                       = var.create_snet ? azurerm_subnet.cdn_snet[0].id : data.azurerm_subnet.cdn_snet[0].id
  force_public_network_access_enabled = true

  static_website = {
    enabled            = true
    index_document     = "index.html"
    error_404_document = "error.html"
  }

  subservices_enabled = {
    blob = true
  }

  blob_features = {
    versioning = true
  }

  tags = var.tags
}

# Data source to retrieve access keys (not exposed by pagopa-dx module)
data "azurerm_storage_account" "cdn" {
  name                = module.cdn_storage_account.name
  resource_group_name = module.cdn_storage_account.resource_group_name
  depends_on = [
    module.cdn_storage_account
  ]
}

# Key Vault certificate for custom domain (apex domain requires custom cert)
data "azurerm_key_vault_certificate" "cdn" {
  count        = var.cdn_certificate_name != null ? 1 : 0
  name         = var.cdn_certificate_name
  key_vault_id = var.key_vault_id
}


###############################################################################
# CDN Front Door (pagopa-dx/azure-cdn/azurerm)
###############################################################################
module "checkout_cdn" {
  source  = "pagopa-dx/azure-cdn/azurerm"
  version = "~> 0.6"

  resource_group_name = azurerm_resource_group.checkout_fe_rg.name

  environment = local.environment

  # Enable WAF for security
  waf_enabled = true

  origins = {
    primary = {
      host_name = module.cdn_storage_account.primary_web_host
      priority  = 1
    }
  }

  custom_domains = [
    {
      host_name = var.host_name
      dns = {
        zone_name                = "${var.dns_zone_prefix}.${var.external_domain}"
        zone_resource_group_name = var.rg_vnet_name
      }
      custom_certificate = {
        key_vault_certificate_versionless_id = var.cdn_certificate_name != null ? data.azurerm_key_vault_certificate.cdn[0].versionless_id : null
        key_vault_name                       = var.key_vault_name != null ? var.key_vault_name : null
        key_vault_resource_group_name        = var.key_vault_resource_group_name != null ? var.key_vault_resource_group_name : null
        key_vault_has_rbac_support           = false
      }
    }
  ]

  diagnostic_settings = {
    enabled                    = var.log_analytics_workspace_enabled
    log_analytics_workspace_id = var.log_analytics_workspace_id
  }

  origin_health_probe               = var.origin_health_probe
  existing_cdn_frontdoor_profile_id = null
  tags                              = var.tags
}

###############################################################################
# Front Door Rules (replaces CDN Classic delivery_rule / global_delivery_rule)
###############################################################################

# Rule 1: Default application rewrite — / → /dashboard/index.html
resource "azurerm_cdn_frontdoor_rule" "default_application" {
  name                      = "${local.basename}cdnfdrule"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 1
  behavior_on_match         = "Continue"

  conditions {
    url_path_condition {
      operator         = "Equal"
      match_values     = ["/"]
      negate_condition = false
    }
  }

  actions {
    url_redirect_action {
      redirect_type        = "Found"
      destination_hostname = var.host_name
      destination_path     = "/dashboard/"
    }
  }
}

# SPA rewrite rules
resource "azurerm_cdn_frontdoor_rule" "spa_rewrite" {
  for_each = { for i, spa in var.spa : spa => i }

  name                      = replace(format("SPA%s", each.key), "-", "")
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + each.value
  behavior_on_match         = "Continue"

  conditions {
    url_path_condition {
      operator         = "BeginsWith"
      match_values     = [format("/%s/", each.key)]
      negate_condition = false
    }

    url_file_extension_condition {
      operator         = "LessThanOrEqual"
      match_values     = ["0"]
      negate_condition = false
    }
  }

  actions {
    url_rewrite_action {
      source_pattern          = format("/%s/", each.key)
      destination             = format("/%s/index.html", each.key)
      preserve_unmatched_path = false
    }
  }
}

# Robots noindex
resource "azurerm_cdn_frontdoor_rule" "robots_no_index" {
  name                      = "robotsNoIndex"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + length(var.spa) + 1
  behavior_on_match         = "Continue"

  conditions {
    url_path_condition {
      operator         = "Equal"
      match_values     = length(var.robots_indexed_paths) > 0 ? var.robots_indexed_paths : ["dummy"]
      negate_condition = true
    }
  }

  actions {
    response_header_action {
      header_action = "Overwrite"
      header_name   = "X-Robots-Tag"
      value         = "noindex, nofollow"
    }
  }
}

# Micro-components no cache (remoteEntry.js)
resource "azurerm_cdn_frontdoor_rule" "microcomponents_no_cache" {
  name                      = "microcomponentsNoCache"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + length(var.spa) + 2
  behavior_on_match         = "Continue"

  conditions {
    url_filename_condition {
      operator         = "Equal"
      match_values     = ["remoteEntry.js"]
      negate_condition = false
    }
  }

  actions {
    response_header_action {
      header_action = "Overwrite"
      header_name   = "Cache-Control"
      value         = "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0"
    }
  }
}

# CORS for /assets/
resource "azurerm_cdn_frontdoor_rule" "cors" {
  name                      = "cors"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + length(var.spa) + 3
  behavior_on_match         = "Continue"

  conditions {
    url_path_condition {
      operator         = "BeginsWith"
      match_values     = local.cors.paths
      negate_condition = false
    }
  }

  actions {
    response_header_action {
      header_action = "Overwrite"
      header_name   = "Access-Control-Allow-Origin"
      value         = "*"
    }
  }
}

# HSTS header
resource "azurerm_cdn_frontdoor_rule" "hsts" {
  name                      = "hsts"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + length(var.spa) + 4
  behavior_on_match         = "Continue"

  actions {
    response_header_action {
      header_action = "Overwrite"
      header_name   = "Strict-Transport-Security"
      value         = "max-age=31536000"
    }
  }
}

resource "azurerm_cdn_frontdoor_rule" "content_security_policy_mixpanel" {
  name                      = "ContentSecurityPolicyMixpanel"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + length(var.spa) + 5
  behavior_on_match         = "Continue"

  actions {
    response_header_action {
      header_action = "Overwrite"
      header_name   = "Content-Security-Policy-Report-Only"
      value         = "default-src 'self'; object-src 'none'; connect-src 'self' https://${var.prefix_api}.${var.dns_zone_prefix}.${var.external_domain}/ https://api-eu.mixpanel.com/track/; "
    }
  }
}

resource "azurerm_cdn_frontdoor_rule" "content_security_policy_fonts" {
  name                      = "ContentSecurityPolicyFonts"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + length(var.spa) + 6
  behavior_on_match         = "Continue"

  actions {
    response_header_action {
      header_action = "Append"
      header_name   = "Content-Security-Policy-Report-Only"
      value         = "script-src 'self'; style-src 'self' 'unsafe-inline' https://${var.host_name}/assets/font/selfhostedfonts.css; worker-src 'none'; font-src 'self' https://${var.host_name}/assets/font/; "
    }
  }
}

resource "azurerm_cdn_frontdoor_rule" "content_security_policy_io" {
  name                      = "ContentSecurityPolicyIO"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + length(var.spa) + 7
  behavior_on_match         = "Continue"

  actions {
    response_header_action {
      header_action = "Append"
      header_name   = "Content-Security-Policy-Report-Only"
      value         = "img-src 'self' https://assets.cdn.io.italia.it https://${var.host_name} data:; "
    }
  }
}

# X-Content-Type-Options
resource "azurerm_cdn_frontdoor_rule" "x_content_type_options" {
  name                      = "xContentTypeOptions"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + length(var.spa) + 8
  behavior_on_match         = "Continue"

  actions {
    response_header_action {
      header_action = "Append"
      header_name   = "X-Content-Type-Options"
      value         = "nosniff"
    }
  }
}


# Content-Security-Policy frame-ancestors
# Azure CDN Front Door rule that enforces Content Security Policy (CSP) headers
# to restrict where this application can be embedded and sandboxed.
#
# Rule Details:
# - name: Identifies the rule as CSP frame-ancestors policy
# - order: Positioned after other rules (2 + number of SPAs + 6)
# - behavior_on_match: "Continue" allows other rules to execute sequentially
#
# Security Headers Applied:
# - frame-ancestors 'none': Prevents embedding in any frame/iframe by default
# - object-src 'none': Disables plugins (Flash, Java, etc.)
# - frame-src 'self' *.{dns_zone_prefix}.{external_domain}: Allows framing only 
#   from same origin and subdomains within the configured external domain
#
# This protects against clickjacking attacks and ensures the application
# can only be embedded within trusted internal subdomains.
resource "azurerm_cdn_frontdoor_rule" "csp_frame_ancestors" {
  name                      = "cspFrameAncestors"
  cdn_frontdoor_rule_set_id = module.checkout_cdn.rule_set_id
  order                     = 2 + length(var.spa) + 9
  behavior_on_match         = "Continue"

  actions {
    response_header_action {
      header_action = "Append"
      header_name   = "Content-Security-Policy"
      value         = format("frame-ancestors 'none'; object-src 'none'; frame-src 'self' *.%s.%s;", var.dns_zone_prefix, var.external_domain)
    }
  }
}

###############################################################################
# Key Vault Secrets
###############################################################################
#tfsec:ignore:azure-keyvault-ensure-secret-expiry
resource "azurerm_key_vault_secret" "selc_web_storage_access_key" {
  name         = "web-storage-access-key"
  value        = data.azurerm_storage_account.cdn.primary_access_key
  content_type = "text/plain"
  key_vault_id = var.key_vault_id
  depends_on   = [module.cdn_storage_account]
}

#tfsec:ignore:azure-keyvault-ensure-secret-expiry
resource "azurerm_key_vault_secret" "selc_web_storage_connection_string" {
  name         = "web-storage-connection-string"
  value        = module.cdn_storage_account.primary_connection_string
  content_type = "text/plain"
  key_vault_id = var.key_vault_id
  depends_on   = [module.cdn_storage_account]
}

#tfsec:ignore:azure-keyvault-ensure-secret-expiry
resource "azurerm_key_vault_secret" "selc_web_storage_blob_connection_string" {
  name         = "web-storage-blob-connection-string"
  value        = data.azurerm_storage_account.cdn.primary_blob_connection_string
  content_type = "text/plain"
  key_vault_id = var.key_vault_id
  depends_on   = [module.cdn_storage_account]
}


