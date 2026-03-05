resource "azurerm_resource_group" "onboarding_fn_rg" {
  name     = "${local.app_name}-rg"
  location = var.location

  tags = var.tags
}

module "onboarding_fn_snet" {
  count                = var.cidr_subnet_selc_onboarding_fn != null ? 1 : 0
  source               = "github.com/pagopa/terraform-azurerm-v3.git//subnet?ref=v8.53.0"
  name                 = format("%s-snet", local.app_name)
  resource_group_name  = data.azurerm_resource_group.rg_vnet.name
  virtual_network_name = data.azurerm_virtual_network.vnet.name
  address_prefixes     = var.cidr_subnet_selc_onboarding_fn

  delegation = {
    name = "default"
    service_delegation = {
      name    = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

data "azurerm_key_vault" "key_vault" {
  resource_group_name = var.key_vault.resource_group_name
  name                = var.key_vault.name
}

module "selc_onboarding_fn" {
  source = "github.com/pagopa/terraform-azurerm-v3.git//function_app?ref=v8.53.0"

  name                = local.app_name
  location            = azurerm_resource_group.onboarding_fn_rg.location
  resource_group_name = azurerm_resource_group.onboarding_fn_rg.name

  enable_healthcheck                       = false
  always_on                                = var.function_always_on
  subnet_id                                = module.onboarding_fn_snet[0].id
  application_insights_instrumentation_key = data.azurerm_application_insights.application_insights.instrumentation_key
  java_version                             = "17"
  runtime_version                          = "~4"

  system_identity_enabled = true
  storage_account_name    = replace(format("%s-sa", local.app_name), "-", "")
  export_keys             = true
  app_service_plan_info   = var.app_service_plan_info
  storage_account_info    = var.storage_account_info

  app_settings = var.app_settings

  tags = var.tags
}

resource "azurerm_key_vault_access_policy" "keyvault_functions_access_policy" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = module.selc_onboarding_fn.system_identity_principal

  secret_permissions = [
    "Get",
  ]
}

data "azurerm_resource_group" "nat_rg" {
  name = "${local.base_domain_name}-nat-rg"
}

data "azurerm_resource_group" "vnet_rg" {
  name = "${local.base_domain_vnet_name}-vnet-rg"
}

data "azurerm_public_ip" "pip_outbound" {
  resource_group_name = var.is_pnpg == true ? data.azurerm_resource_group.nat_rg.name : data.azurerm_resource_group.vnet_rg.name
  name                = var.is_pnpg == true ? "${local.base_domain_name}-pip-outbound" : "${local.base_domain_name}-aksoutbound-pip-01"
}

data "azurerm_nat_gateway" "nat_gateway" {
  name                = "${local.base_domain_name}-nat_gw"
  resource_group_name = data.azurerm_resource_group.nat_rg.name
}

resource "azurerm_nat_gateway_public_ip_association" "functions_pip_nat_gateway" {
  nat_gateway_id       = data.azurerm_nat_gateway.nat_gateway.id
  public_ip_address_id = data.azurerm_public_ip.pip_outbound.id
}

resource "azurerm_subnet_nat_gateway_association" "functions_subnet_nat_gateway" {
  subnet_id      = module.onboarding_fn_snet[0].id
  nat_gateway_id = data.azurerm_nat_gateway.nat_gateway.id
}

resource "azurerm_key_vault_secret" "fn_primary_key" {
  name         = "fn-onboarding-primary-key"
  value        = module.selc_onboarding_fn.primary_key
  content_type = "text/plain"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}