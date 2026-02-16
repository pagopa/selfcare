## VPN subnet
module "vpn_snet" {
  source                            = "github.com/pagopa/terraform-azurerm-v4.git//subnet?ref=v7.26.4"
  name                              = "GatewaySubnet"
  address_prefixes                  = var.cidr_subnet_vpn
  resource_group_name               = var.rg_vnet_name
  virtual_network_name              = var.vnet_name
  service_endpoints                 = []
  private_endpoint_network_policies = var.private_endpoint_network_policies
}

data "azuread_application" "vpn_app" {
  display_name = "${var.project}-app-vpn"
}

module "vpn" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//vpn_gateway?ref=v7.26.4"

  name                = "${var.project}-vpn"
  location            = var.location
  resource_group_name = var.vnet_name
  sku                 = var.vpn_sku
  pip_sku             = var.vpn_pip_sku
  subnet_id           = var.vpn_snet_id

  vpn_client_configuration = [
    {
      address_space         = ["172.16.1.0/24"],
      vpn_client_protocols  = ["OpenVPN"],
      aad_audience          = data.azuread_application.vpn_app.client_id
      aad_issuer            = format("https://sts.windows.net/%s/", var.tenant_id)
      aad_tenant            = format("https://login.microsoftonline.com/%s", var.tenant_id)
      radius_server_address = null
      radius_server_secret  = null
      revoked_certificate   = []
      root_certificate      = []
    }
  ]

  # Security Logs
  sec_log_analytics_workspace_id = var.env_short == "p" ? var.sec_workspace_id : null
  sec_storage_id                 = var.env_short == "p" ? var.sec_storage_id : null

  tags = var.tags
}

## DNS Forwarder
module "dns_forwarder_snet" {
  source                            = "github.com/pagopa/terraform-azurerm-v4.git//subnet?ref=v7.26.4"
  name                              = "${var.project}-dns-forwarder-snet"
  address_prefixes                  = var.cidr_subnet_dns_forwarder
  resource_group_name               = var.rg_vnet_name
  virtual_network_name              = var.vnet_name
  private_endpoint_network_policies = var.private_endpoint_network_policies
}

module "dns_forwarder" {
  source              = "git::https://github.com/pagopa/terraform-azurerm-v4.git//dns_forwarder_vm_image?ref=v7.26.4"
  resource_group_name = var.rg_vnet_name
  location            = var.location
  image_name          = "${var.project}-dns-forwarder-ubuntu2204-image"
  image_version       = "v1"
  subscription_id     = var.subscription_id
  prefix              = var.project
}

# with default image
module "dns_forwarder_vpn" {

  source              = "git::https://github.com/pagopa/terraform-azurerm-v4.git//dns_forwarder_scale_set_vm?ref=v7.26.4"
  name                = "${var.project}-dns-forwarder"
  resource_group_name = var.rg_vnet_name
  subnet_id           = module.dns_forwarder_snet.id
  subscription_name   = var.subscription_name
  subscription_id     = var.subscription_id
  location            = var.location
  source_image_name   = "${var.project}-dns-forwarder-ubuntu2204-image-v1"

  tags = var.tags
}


# DNS FORWARDER FOR DISASTER RECOVERY

#
# DNS Forwarder
#
module "dns_forwarder_pair_subnet" {
  source                            = "github.com/pagopa/terraform-azurerm-v4.git//subnet?ref=v7.26.4"
  name                              = "${var.project_pair}-dnsforwarder-snet"
  address_prefixes                  = var.cidr_subnet_pair_dnsforwarder
  resource_group_name               = var.rg_pair_vnet_name
  virtual_network_name              = var.vnet_pair_name
  private_endpoint_network_policies = var.private_endpoint_network_policies
}

output "subnet_pair_id" {
  value = module.dns_forwarder_pair_subnet.id
}

resource "random_id" "pair_dns_forwarder_hash" {
  byte_length = 3
}


module "vpn_pair_dns_forwarder" {
  source              = "git::https://github.com/pagopa/terraform-azurerm-v4.git//dns_forwarder_vm_image?ref=v7.26.4"
  resource_group_name = var.rg_pair_vnet_name
  location            = var.location_pair
  image_name          = "${var.project_pair}-dns-forwarder-ubuntu2204-image"
  image_version       = "v1"
  subscription_id     = var.subscription_id
  prefix              = var.project_pair
}

module "dns_forwarder_pair_vpn" {

  source              = "git::https://github.com/pagopa/terraform-azurerm-v4.git//dns_forwarder_scale_set_vm?ref=v7.26.4"
  name                = "${var.project_pair}-dns-forwarder"
  resource_group_name = var.rg_pair_vnet_name
  subnet_id           = module.dns_forwarder_pair_subnet.id
  subscription_name   = var.subscription_name
  subscription_id     = var.subscription_id
  location            = var.location_pair
  source_image_name   = "${var.project_pair}-dns-forwarder-ubuntu2204-image-v1"

  tags = var.tags
}