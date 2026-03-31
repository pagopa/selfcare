
resource "azurerm_resource_group" "fn_rg" {
  name     = "${var.functions_name}-rg"
  location = var.location
  tags     = var.tags
}

resource "azurerm_subnet" "fn_snet" {
  name                 = "${var.functions_name}-snet"
  resource_group_name  = var.vnet_resource_group_name
  virtual_network_name = var.vnet_name
  address_prefixes     = var.subnet_cidr

  delegation {
    name = "default"

    service_delegation {
      name    = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

resource "azurerm_service_plan" "fn_plan" {
  name                = "${var.functions_name}-plan"
  location            = azurerm_resource_group.fn_rg.location
  resource_group_name = azurerm_resource_group.fn_rg.name

  os_type      = "Linux"
  sku_name     = var.service_plan_sku
  worker_count = var.service_plan_worker_count

  tags = var.tags
}

resource "azurerm_storage_account" "fn_storage" {
  name                = replace("${var.functions_name}-sa", "-", "")
  location            = azurerm_resource_group.fn_rg.location
  resource_group_name = azurerm_resource_group.fn_rg.name

  account_kind             = "StorageV2"
  account_tier             = "Standard"
  account_replication_type = var.replication_type
  access_tier              = "Hot"

  public_network_access_enabled = true

  tags = var.tags

  lifecycle {
    ignore_changes = [
      public_network_access_enabled,
    ]
  }
}

resource "azurerm_linux_function_app" "fn" {
  name                = var.functions_name
  location            = azurerm_resource_group.fn_rg.location
  resource_group_name = azurerm_resource_group.fn_rg.name

  service_plan_id            = azurerm_service_plan.fn_plan.id
  storage_account_name       = azurerm_storage_account.fn_storage.name
  storage_account_access_key = azurerm_storage_account.fn_storage.primary_access_key

  functions_extension_version = "~4"
  virtual_network_subnet_id   = azurerm_subnet.fn_snet.id
  https_only                  = true

  identity {
    type = "SystemAssigned"
  }

  site_config {
    always_on              = var.always_on
    vnet_route_all_enabled = true

    application_stack {
      java_version = "17"
    }
  }

  app_settings = var.app_settings

  tags = var.tags

  lifecycle {
    ignore_changes = [
      app_settings,
    ]
  }
}

resource "azurerm_key_vault_access_policy" "fn_keyvault_access_policy" {
  key_vault_id = var.key_vault_id
  tenant_id    = var.tenant_id
  object_id    = azurerm_linux_function_app.fn.identity[0].principal_id

  secret_permissions = [
    "Get",
  ]
}

data "azurerm_function_app_host_keys" "fn" {
  name                = azurerm_linux_function_app.fn.name
  resource_group_name = azurerm_resource_group.fn_rg.name
}

resource "azurerm_key_vault_secret" "fn_primary_key" {
  name         = "fn-onboarding-primary-key"
  value        = data.azurerm_function_app_host_keys.fn.default_function_key
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
  subnet_id      = azurerm_subnet.fn_snet.id
  nat_gateway_id = data.azurerm_nat_gateway.fn_nat_gateway.id
}