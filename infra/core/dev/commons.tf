###############################################################################
# network
###############################################################################
module "network" {
  source = "../_modules/network"

  prefix              = local.prefix
  env_short           = local.env_short
  location            = local.location
  location_short      = local.location_short
  location_pair       = local.location_pair
  location_pair_short = local.location_pair_short
  tags                = local.tags

  cidr_vnet                         = local.cidr_vnet
  cidr_pair_vnet                    = local.cidr_pair_vnet
  cidr_aks_platform_vnet            = local.cidr_aks_platform_vnet
  cidr_subnet_private_endpoints     = local.cidr_subnet_private_endpoints
  private_endpoint_network_policies = local.private_endpoint_network_policies
  ddos_protection_plan              = null
  aks_platform_env                  = local.aks_platform_env
}

###############################################################################
# key_vault
###############################################################################
module "key_vault" {
  source = "../_modules/key_vault"

  prefix    = local.prefix
  env_short = local.env_short
  location  = local.location
  tags      = local.tags

  azdo_sp_tls_cert_enabled                         = local.azdo_sp_tls_cert_enabled
  azuread_service_principal_azure_cdn_frontdoor_id = "f3b3f72f-4770-47a5-8c1e-aa298003be12"
  app_gateway_api_certificate_name                 = local.app_gateway_api_certificate_name
  app_gateway_api_pnpg_certificate_name            = local.app_gateway_api_pnpg_certificate_name
}

###############################################################################
# dns_private
###############################################################################
module "dns_private" {
  source = "../_modules/dns_private"

  prefix    = local.prefix
  env_short = local.env_short
  env       = local.env
  tags      = local.tags

  reverse_proxy_ip               = local.reverse_proxy_ip
  redis_private_endpoint_enabled = local.redis_private_endpoint_enabled

  rg_vnet_name           = module.network.rg_vnet_name
  vnet_id                = module.network.vnet_id
  vnet_name              = module.network.vnet_name
  vnet_pair_id           = module.network.vnet_pair_id
  vnet_pair_name         = module.network.vnet_pair_name
  vnet_aks_platform_id   = module.network.vnet_aks_platform_id
  vnet_aks_platform_name = module.network.vnet_aks_platform_name
}

###############################################################################
# dns_public
###############################################################################
module "dns_public" {
  source = "../_modules/dns_public"

  prefix    = local.prefix
  env_short = local.env_short
  tags      = local.tags

  dns_zone_prefix         = local.dns_zone_prefix
  external_domain         = local.external_domain
  dns_zone_prefix_ar      = local.dns_zone_prefix_ar
  dns_ns_interop_selfcare = local.dns_ns_interop_selfcare

  rg_vnet_name                 = module.network.rg_vnet_name
  appgateway_public_ip_address = module.network.appgateway_public_ip_address
}

###############################################################################
# nat
###############################################################################
module "nat" {
  source = "../_modules/nat"

  prefix    = local.prefix
  env_short = local.env_short
  location  = local.location
  tags      = local.tags
}

###############################################################################
# log_analytics (LAW + AppInsights - separate to break CDN/monitor cycle)
###############################################################################
module "log_analytics" {
  source = "../_modules/log_analytics"

  prefix    = local.prefix
  env_short = local.env_short
  location  = local.location
  tags      = local.tags

  key_vault_id          = module.key_vault.key_vault_id
  law_sku               = local.law_sku
  law_retention_in_days = local.law_retention_in_days
  law_daily_quota_gb    = local.law_daily_quota_gb
}

###############################################################################
# cdn (Front Door — migrated from CDN Classic)
###############################################################################
# module "cdn_fd" {
#   source = "../_modules/cdn_fd"

#   prefix    = local.prefix
#   env_short = local.env_short
#   location  = local.location
#   tags      = local.tags

#   dns_zone_prefix      = local.dns_zone_prefix
#   external_domain      = local.external_domain
#   robots_indexed_paths = local.robots_indexed_paths

#   log_analytics_workspace_id = module.log_analytics.log_analytics_workspace_id
#   key_vault_id               = module.key_vault.key_vault_id
#   tenant_id                  = module.key_vault.tenant_id
#   rg_vnet_name               = module.network.rg_vnet_name
# }

module "cdn" {
  source = "../_modules/cdn"

  prefix          = local.prefix
  env_short       = local.env_short
  location        = local.location
  location_short  = local.location_short
  tags            = local.tags
  domain          = local.app_domain
  app_name        = "checkout"
  instance_number = "01"

  dns_zone_prefix      = local.dns_zone_prefix
  external_domain      = local.external_domain
  robots_indexed_paths = local.robots_indexed_paths
  storage_use_case     = "development"

  log_analytics_workspace_id    = module.log_analytics.log_analytics_workspace_id
  key_vault_id                  = module.key_vault.key_vault_id
  key_vault_name                = module.key_vault.key_vault_name
  key_vault_resource_group_name = module.key_vault.key_vault_resource_group_name
  cdn_certificate_name          = replace("${local.dns_zone_prefix}.${local.external_domain}", ".", "-")
  vnet_name                     = module.network.vnet_name
  rg_vnet_name                  = module.network.rg_vnet_name
  cidr_subnet_cdn               = local.cidr_subnet_cdn
}

resource "null_resource" "cdn_storage_copy" {
  for_each = toset(["$web", "selc-${local.env_short}-product", "selc-openapi"])

  provisioner "local-exec" {
    command = <<EOT
        az storage blob copy start-batch \
        --account-name "${module.cdn.storage_name}" \
        --account-key "${module.cdn.storage_primary_access_key}" \
        --destination-container '${each.value}' \
        --source-account-name "${module.cdn.old_storage_account_name}" \
        --source-account-key "${module.cdn.old_storage_account_primary_access_key}" \
        --source-container '${each.value}'
    EOT
  }
}

###############################################################################
# monitor (action groups, web tests, alerts)
###############################################################################
module "monitor" {
  source = "../_modules/monitor"

  prefix    = local.prefix
  env_short = local.env_short
  location  = local.location
  tags      = local.tags

  subscription_id = module.key_vault.subscription_id
  key_vault_id    = module.key_vault.key_vault_id

  # From log_analytics module
  monitor_rg_name           = module.log_analytics.monitor_rg_name
  monitor_rg_location       = module.log_analytics.monitor_rg_location
  application_insights_id   = module.log_analytics.application_insights_id
  application_insights_name = module.log_analytics.application_insights_name

  # Web test URLs
  dns_a_api_fqdn      = module.dns_public.dns_a_api_fqdn
  dns_a_api_pnpg_fqdn = module.dns_public.dns_a_api_pnpg_fqdn
  #fixme: these should be outputs from the cdn module, but that would create a cycle since the cdn module needs the monitor for log analytics workspace
  cdn_fqdn = "dev.selfcare.pagopa.it"
  # module.cdn.fqdn

  # Selfcare status secrets (from key_vault secrets query)
  selfcare_status_dev_email = try(module.key_vault.secrets_selfcare_status_dev["alert-selfcare-status-dev-email"].value, "")
  selfcare_status_dev_slack = try(module.key_vault.secrets_selfcare_status_dev["alert-selfcare-status-dev-slack"].value, "")
}

###############################################################################
# events
###############################################################################
module "events" {
  source = "../_modules/events"

  prefix    = local.prefix
  env_short = local.env_short
  location  = local.location
  tags      = local.tags

  private_endpoint_network_policies = local.private_endpoint_network_policies

  rg_vnet_name = module.network.rg_vnet_name
  vnet_id      = module.network.vnet_id
  vnet_name    = module.network.vnet_name
  key_vault_id = module.key_vault.key_vault_id

  action_group_error_id = module.monitor.action_group_error_id
  action_group_slack_id = module.monitor.action_group_slack_id
  action_group_email_id = module.monitor.action_group_email_id

  privatelink_servicebus_windows_net_ids   = [module.dns_private.privatelink_servicebus_windows_net_id]
  privatelink_servicebus_windows_net_names = [module.dns_private.privatelink_servicebus_windows_net_name]

  cidr_subnet_eventhub              = local.cidr_subnet_eventhub
  eventhub_auto_inflate_enabled     = local.eventhub_auto_inflate_enabled
  eventhub_sku_name                 = local.eventhub_sku_name
  eventhub_capacity                 = local.eventhub_capacity
  eventhub_maximum_throughput_units = local.eventhub_maximum_throughput_units
  eventhubs                         = local.eventhubs
  eventhub_ip_rules                 = local.eventhub_ip_rules
  eventhub_alerts_enabled           = local.eventhub_alerts_enabled
}

###############################################################################
# appgateway
###############################################################################
module "appgateway" {
  source = "../_modules/appgateway"

  prefix    = local.prefix
  env_short = local.env_short
  tags      = local.tags

  dns_zone_prefix                       = local.dns_zone_prefix
  external_domain                       = local.external_domain
  aks_platform_env                      = local.aks_platform_env
  auth_ms_private_dns_suffix            = local.auth_ms_private_dns_suffix
  ca_pnpg_suffix_dns_private_name       = local.ca_pnpg_suffix_dns_private_name
  app_gateway_api_certificate_name      = local.app_gateway_api_certificate_name
  app_gateway_api_pnpg_certificate_name = local.app_gateway_api_pnpg_certificate_name
  private_endpoint_network_policies     = local.private_endpoint_network_policies

  rg_vnet_name            = module.network.rg_vnet_name
  rg_vnet_location        = module.network.rg_vnet_location
  vnet_name               = module.network.vnet_name
  appgateway_public_ip_id = module.network.appgateway_public_ip_id
  cidr_subnet_appgateway  = local.cidr_subnet_appgateway

  key_vault_id           = module.key_vault.key_vault_id
  appgateway_identity_id = module.key_vault.appgateway_identity_id

  action_group_error_id = module.monitor.action_group_error_id
  action_group_slack_id = module.monitor.action_group_slack_id
  action_group_email_id = module.monitor.action_group_email_id
}

###############################################################################
# storage
###############################################################################
module "storage" {
  source = "../_modules/storage"

  prefix    = local.prefix
  env_short = local.env_short
  env       = local.env
  location  = local.location
  tags      = local.tags

  adgroup_developers_object_id = module.key_vault.adgroup_developers_object_id
  adgroup_admin_object_id      = module.key_vault.adgroup_admin_object_id
}

###############################################################################
# vpn
###############################################################################

module "vpn" {
  source = "../_modules/vpn"

  env_short     = local.env_short
  project       = local.project
  project_pair  = local.project_pair
  location      = local.location
  location_pair = local.location_pair
  tags          = local.tags

  vnet_name    = module.network.vnet_name
  rg_vnet_name = module.network.rg_vnet_name

  rg_pair_vnet_name = module.network.rg_pair_vnet_name
  vnet_pair_name    = module.network.vnet_pair_name

  vpn_sku     = local.vpn_sku
  vpn_pip_sku = local.vpn_pip_sku

  cidr_subnet_vpn           = local.cidr_subnet_vpn
  cidr_subnet_dns_forwarder = local.cidr_subnet_dns_forwarder

  cidr_subnet_pair_dnsforwarder = local.cidr_subnet_pair_dnsforwarder

  private_endpoint_network_policies = local.private_endpoint_network_policies

  subscription_id   = module.key_vault.subscription_id
  subscription_name = module.key_vault.subscription_name
  tenant_id         = module.key_vault.tenant_id



  sec_workspace_id = local.env_short == "p" ? module.key_vault.secrets_sec_workspace_id : null
  sec_storage_id   = local.env_short == "p" ? module.key_vault.secrets_sec_storage_id : null
}


###############################################################################
# cosmos db
###############################################################################

module "cosmos_db" {
  source = "../_modules/cosmos_db"

  prefix    = local.prefix
  env_short = local.env_short
  location  = local.location
  tags      = local.tags
  project   = local.project

  external_domain = local.external_domain

  # Network
  rg_vnet_name                 = module.network.rg_vnet_name
  vnet_name                    = module.network.vnet_name
  cidr_subnet_cosmosdb_mongodb = local.cidr_subnet_cosmosdb_mongodb

  # Key Vault
  key_vault_id = module.key_vault.key_vault_id

  # Private DNS
  privatelink_mongo_cosmos_azure_com_id = module.dns_private.privatelink_mongo_cosmos_azure_com_id

  # CosmosDB MongoDB
  cosmosdb_mongodb_extra_capabilities               = local.cosmosdb_mongodb_extra_capabilities
  cosmosdb_mongodb_main_geo_location_zone_redundant = local.cosmosdb_mongodb_main_geo_location_zone_redundant
}


###############################################################################
# default_roleassignment_rg
###############################################################################

module "default_roleassignment" {
  source = "../_modules/roles"

  location = local.location
  tags     = local.tags
}


###############################################################################
# resources
###############################################################################

# module "resources" {
#   source = "../_modules/resources"

#   env = local.env

#   # CDN
#   checkout_cdn_name                       = module.cdn.name
#   checkout_cdn_storage_primary_access_key = module.cdn.storage_primary_access_key
#   checkout_fe_rg_name                     = module.cdn.checkout_fe_rg_name

#   # Contract Storage — TODO: expose these from the appropriate module
#   selc_contracts_storage_name               = "" # module.<contracts_storage>.name
#   selc_contracts_storage_primary_access_key = "" # module.<contracts_storage>.primary_access_key
#   selc_contracts_container_name             = "" # <contracts_container>.name
# }


###############################################################################
# assets
###############################################################################

module "assets" {
  source = "../_modules/assets"

  env = local.env
  # CDN
  checkout_cdn_name                       = module.cdn.storage_name
  checkout_endpoint_name                  = module.cdn.name
  checkout_cdn_storage_primary_access_key = module.cdn.storage_primary_access_key
  checkout_fe_rg_name                     = module.cdn.checkout_fe_rg_name
}


###############################################################################
# one trust
###############################################################################

module "one_trust" {
  source = "../_modules/one_trust"

  env                                     = local.env
  checkout_cdn_name                       = module.cdn.storage_name
  checkout_endpoint_name                  = module.cdn.name
  checkout_cdn_storage_primary_access_key = module.cdn.storage_primary_access_key
  checkout_fe_rg_name                     = module.cdn.checkout_fe_rg_name
}

###############################################################################
# Contract storage
###############################################################################

module "contracts_storage" {
  source = "../_modules/deprecate/storage"

  prefix    = local.prefix
  env_short = local.env_short
  env       = local.env
  location  = local.location
  tags      = local.tags

  rg_vnet_name = module.network.rg_vnet_name
  vnet_name    = module.network.vnet_name
  project      = local.project
  key_vault_id = module.key_vault.key_vault_id

  contracts_enable_versioning          = local.contracts_enable_versioning
  contracts_advanced_threat_protection = local.contracts_advanced_threat_protection
  contracts_delete_retention_days      = local.contracts_delete_retention_days

  cidr_subnet_contract_storage      = local.cidr_subnet_contract_storage
  private_endpoint_network_policies = local.private_endpoint_network_policies
  private_dns_zone_ids              = [module.dns_private.privatelink_blob_core_windows_net_id]

  logs_delete_retention_days = 1
  cidr_subnet_logs_storage   = local.cidr_subnet_logs_storage

  logs_public_network_access_enabled = true
}


###############################################################################
# Azure DevOps Agent
###############################################################################
module "azure_devops_agent" {
  source = "../_modules/azure_devops_agent"

  project  = local.project
  location = local.location
  tags     = local.tags

  enable_azdoa                 = true
  enable_iac_pipeline          = true
  enable_app_projects_pipeline = true

  cidr_subnet_azdoa = local.cidr_subnet_azdoa
  rg_vnet_name      = module.network.rg_vnet_name
  vnet_name         = module.network.vnet_name
  subscription_id   = module.key_vault.subscription_id
  env_short         = local.env_short
  key_vault_id      = module.key_vault.key_vault_id
  tenant_id         = module.key_vault.tenant_id

  private_endpoint_network_policies = local.private_endpoint_network_policies
}