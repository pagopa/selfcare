module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"
  domain    = "ar"

  dns_zone_prefix                = "dev.selfcare"
  api_dns_zone_prefix            = "api.dev.selfcare"
  private_dns_name_domain        = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-cae-002"
  ca_resource_group_name         = "selc-d-container-app-002-rg"
  container_app_min_replicas     = 0
}

data "azurerm_resource_group" "monitor" {
  name = "${module.local.config.project}-monitor-rg"
}

data "azurerm_log_analytics_workspace" "availability" {
  name                = "${module.local.config.project}-law"
  resource_group_name = data.azurerm_resource_group.monitor.name
}

# Synthetic monitoring account: private Table endpoint and public network access disabled.
data "azurerm_storage_account" "availability" {
  name                = "selc${module.local.config.env_short}weusynthmon"
  resource_group_name = "${module.local.config.project}-synthetic-monitoring-rg"
}

data "azurerm_application_gateway" "api" {
  name                = "${module.local.config.project}-app-gw"
  resource_group_name = module.local.config.resource_group_name_vnet
}

module "log_availability_runner" {
  source = "../../_modules/log_availability_runner"

  env_short                           = module.local.config.env_short
  environment                         = upper(module.local.config.env)
  location                            = data.azurerm_resource_group.monitor.location
  monitor_resource_group_name         = data.azurerm_resource_group.monitor.name
  container_app_resource_group_name   = module.local.config.ca_resource_group_name
  container_app_environment_name      = module.local.config.container_app_environment_name
  container_app                       = module.local.config.container_app
  key_vault_resource_group_name       = module.local.config.key_vault_resource_group_name
  key_vault_name                      = module.local.config.key_vault_name
  log_analytics_workspace_guid        = data.azurerm_log_analytics_workspace.availability.workspace_id
  log_analytics_workspace_resource_id = data.azurerm_log_analytics_workspace.availability.id
  application_gateway_resource_id     = data.azurerm_application_gateway.api.id
  storage_account_id                  = data.azurerm_storage_account.availability.id
  storage_account_name                = data.azurerm_storage_account.availability.name
  image_tag                           = var.image_tag
  tags                                = module.local.config.tags
}
