resource "azurerm_resource_group" "fn_rg" {
  name     = "${var.functions_name}-rg"
  location = var.location
  tags     = var.tags
}

module "onboarding_fn_snet" {
  count  = var.subnet_cidr != null && length(var.subnet_cidr) > 0 ? 1 : 0
  source = "github.com/pagopa/terraform-azurerm-v3.git//subnet?ref=v8.53.0"

  name                 = format("%s-snet", var.functions_name)
  resource_group_name  = var.vnet_resource_group_name
  virtual_network_name = var.vnet_name
  address_prefixes     = var.subnet_cidr

  delegation = {
    name = "default"
    service_delegation = {
      name    = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

module "selc_onboarding_fn" {
  source = "github.com/pagopa/terraform-azurerm-v3.git//function_app?ref=v8.53.0"

  name                = var.functions_name
  location            = azurerm_resource_group.fn_rg.location
  resource_group_name = azurerm_resource_group.fn_rg.name

  enable_healthcheck                       = false
  always_on                                = var.always_on
  subnet_id                                = module.onboarding_fn_snet[0].id
  application_insights_instrumentation_key = local.resolved_appinsights_key
  java_version                             = "17"
  runtime_version                          = "~4"

  system_identity_enabled = true
  storage_account_name    = replace(format("%s-sa", var.functions_name), "-", "")
  export_keys             = true
  app_service_plan_info   = local.app_service_plan_info
  storage_account_info    = local.storage_account_info

  app_settings = var.app_settings

  tags = var.tags
}

resource "azurerm_key_vault_access_policy" "fn_keyvault_access_policy" {
  key_vault_id = var.key_vault_id
  tenant_id    = var.tenant_id
  object_id    = module.selc_onboarding_fn.system_identity_principal

  secret_permissions = [
    "Get",
  ]
}

resource "azurerm_key_vault_secret" "fn_primary_key" {
  name         = "fn-onboarding-primary-key"
  value        = module.selc_onboarding_fn.primary_key
  content_type = "text/plain"
  key_vault_id = var.key_vault_id
}

data "azurerm_resource_group" "fn_nat_rg" {
  name = var.nat_resource_group_name
}

data "azurerm_nat_gateway" "fn_nat_gateway" {
  name                = var.nat_gateway_name
  resource_group_name = data.azurerm_resource_group.fn_nat_rg.name
}

resource "azurerm_subnet_nat_gateway_association" "fn_subnet_nat_gateway" {
  subnet_id      = module.onboarding_fn_snet[0].id
  nat_gateway_id = data.azurerm_nat_gateway.fn_nat_gateway.id
}

data "azurerm_key_vault_secret" "appinsights_connection_string" {
  count = var.application_insights_connection_string == null ? 1 : 0

  name         = var.application_insights_connection_string_secret_name
  key_vault_id = var.key_vault_id
}

locals {
  resolved_appinsights_connection_string = var.application_insights_connection_string != null ? var.application_insights_connection_string : data.azurerm_key_vault_secret.appinsights_connection_string[0].value
  resolved_appinsights_key = var.application_insights_key != null ? var.application_insights_key : element([
    for part in split(";", local.resolved_appinsights_connection_string) : trimprefix(part, "InstrumentationKey=")
    if startswith(part, "InstrumentationKey=")
  ], 0)

  app_service_plan_info = {
    kind                         = "Linux"
    sku_size                     = var.service_plan_sku
    maximum_elastic_worker_count = 0
    worker_count                 = var.service_plan_worker_count
    zone_balancing_enabled       = false
  }

  storage_account_info = {
    account_kind                      = "StorageV2"
    account_tier                      = "Standard"
    account_replication_type          = var.replication_type
    access_tier                       = "Hot"
    advanced_threat_protection_enable = true
    use_legacy_defender_version       = true
    public_network_access_enabled     = false
  }
}
