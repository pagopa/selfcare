module "key_vault_rbac" {
  source = "../_modules/key_vault_rbac"

  prefix              = local.prefix
  env_short           = local.env_short
  domain              = "ar"
  location            = module.key_vault.sec_rg_location
  resource_group_name = module.key_vault.sec_rg_name
  tenant_id           = module.key_vault.tenant_id
  tags                = local.tags

  networking = {
    resource_group_name        = module.network.rg_vnet_name
    virtual_network_id         = module.network.vnet_id
    private_endpoint_subnet_id = module.network.private_endpoints_subnet_id
  }
}
