# APIM subnet
module "apim_snet" {
  source               = "github.com/pagopa/terraform-azurerm-v4.git//subnet?ref=v10.9.0"
  name                 = "${local.project}-apim-v2-snet"
  resource_group_name  = "${local.project}-vnet-rg"
  virtual_network_name = data.azurerm_virtual_network.vnet.name
  address_prefixes     = var.cidr_subnet_apim

  private_endpoint_network_policies = "Enabled"
  service_endpoints                 = ["Microsoft.Web"]
}

resource "azurerm_network_security_group" "nsg_apim" {
  name                = format("%s-apim-v2-nsg", local.project)
  resource_group_name = format("%s-vnet-rg", local.project)
  location            = var.location

  security_rule {
    name                       = "managementapim"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "3443"
    source_address_prefix      = "ApiManagement"
    destination_address_prefix = "VirtualNetwork"
  }

  tags = var.tags
}

resource "azurerm_subnet_network_security_group_association" "snet_nsg" {
  subnet_id                 = module.apim_snet.id
  network_security_group_id = azurerm_network_security_group.nsg_apim.id
}

resource "azurerm_resource_group" "rg_api" {
  name     = "${local.project}-api-v2-rg"
  location = var.location

  tags = var.tags
}

resource "azurerm_key_vault_access_policy" "api_management_policy" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = module.apim.principal_id

  key_permissions         = []
  secret_permissions      = ["Get", "List"]
  certificate_permissions = ["Get", "List"]
  storage_permissions     = []
}

resource "azurerm_key_vault_access_policy" "api_management_policy_pnpg" {
  key_vault_id = data.azurerm_key_vault.key_vault_pnpg.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = module.apim.principal_id

  key_permissions         = []
  secret_permissions      = ["Get", "List"]
  certificate_permissions = ["Get", "List"]
  storage_permissions     = []
}

resource "azurerm_api_management_custom_domain" "api_custom_domain" {
  api_management_id = module.apim.id

  gateway {
    host_name                = local.api_domain
    key_vault_certificate_id = data.azurerm_key_vault_certificate.app_gw_platform.versionless_secret_id
    # replace(
    #   data.azurerm_key_vault_certificate.app_gw_platform.secret_id,
    #   "/${data.azurerm_key_vault_certificate.app_gw_platform.version}",
    #   ""
    # )
  }
}

###########################
## Api Management (apim) ##
###########################

module "apim" {
  source               = "github.com/pagopa/terraform-azurerm-v4.git//api_management?ref=v10.9.0"
  subnet_id            = module.apim_snet.id
  location             = azurerm_resource_group.rg_api.location
  name                 = "${local.project}-apim-v2"
  resource_group_name  = azurerm_resource_group.rg_api.name
  publisher_name       = var.apim_publisher_name
  publisher_email      = data.azurerm_key_vault_secret.apim_publisher_email.value
  sku_name             = var.apim_sku
  virtual_network_type = "Internal"

  redis_connection_string = null
  redis_cache_id          = null

  # This enables the Username and Password Identity Provider
  sign_up_enabled = false
  lock_enable     = false

  management_logger_applicaiton_insight_enabled = var.application_insight_enabled

  application_insights = {
    enabled             = true
    instrumentation_key = data.azurerm_application_insights.ai.instrumentation_key
  }

  diagnostic_sampling_percentage = var.diagnostic_sampling_percentage

  xml_content = file("${path.module}/root_policy.xml")

  tags = var.tags
}
