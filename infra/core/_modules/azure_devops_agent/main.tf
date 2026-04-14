resource "azurerm_resource_group" "azdo_rg" {
  count    = var.enable_azdoa ? 1 : 0
  name     = format("%s-azdoa-rg", var.project)
  location = var.location

  tags = var.tags
}

module "azdoa_snet" {
  source                            = "github.com/pagopa/terraform-azurerm-v4.git//subnet?ref=v9.6.1"
  count                             = var.enable_azdoa ? 1 : 0
  name                              = format("%s-azdoa-snet", var.project)
  address_prefixes                  = var.cidr_subnet_azdoa
  resource_group_name               = var.rg_vnet_name
  virtual_network_name              = var.vnet_name
  private_endpoint_network_policies = var.private_endpoint_network_policies

  service_endpoints = [
    "Microsoft.Storage",
  ]
}

module "azdoa_li" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//azure_devops_agent?ref=v9.6.1"
  count               = var.enable_azdoa ? 1 : 0
  name                = "${var.project}-azdoa-vmss-ubuntu-app"
  resource_group_name = azurerm_resource_group.azdo_rg[0].name
  subnet_id           = module.azdoa_snet[0].id
  # subscription_name   = data.azurerm_subscription.current.display_name
  subscription_id   = var.subscription_id
  location          = var.location
  image_type        = "custom" # enables usage of "source_image_name"
  source_image_name = "selc-${var.env_short}-azdo-agent-ubuntu2204-image-v1"
  vm_sku            = var.azdo_agent_vm_sku

  tags = var.tags
}

module "azdoa_li_infra" {
  source              = "github.com/pagopa/terraform-azurerm-v4.git//azure_devops_agent?ref=v9.6.1"
  count               = var.enable_azdoa ? 1 : 0
  name                = "${var.project}-azdoa-vmss-ubuntu-infra"
  resource_group_name = azurerm_resource_group.azdo_rg[0].name
  subnet_id           = module.azdoa_snet[0].id
  # subscription_name   = data.azurerm_subscription.current.display_name
  subscription_id   = var.subscription_id
  location          = var.location
  image_type        = "custom" # enables usage of "source_image_name"
  source_image_name = "selc-${var.env_short}-azdo-agent-ubuntu2204-image-v1"
  vm_sku            = var.azdo_agent_vm_sku

  tags = var.tags
}


resource "azurerm_key_vault_access_policy" "azdevops_iac_policy" {
  count        = var.enable_iac_pipeline ? 1 : 0
  key_vault_id = var.key_vault_id
  tenant_id    = var.tenant_id
  object_id    = var.iac_principal_object_id

  secret_permissions      = ["Get", "List", "Set", ]
  certificate_permissions = ["SetIssuers", "DeleteIssuers", "Purge", "List", "Get"]
  storage_permissions     = []
}


resource "azurerm_key_vault_access_policy" "azdevops_app_projects_policy" {
  count        = var.enable_app_projects_pipeline ? 1 : 0
  key_vault_id = var.key_vault_id
  tenant_id    = var.tenant_id
  object_id    = var.app_projects_principal_object_id

  secret_permissions      = ["Get", "List"]
  certificate_permissions = []
  storage_permissions     = []
  key_permissions         = []
}
