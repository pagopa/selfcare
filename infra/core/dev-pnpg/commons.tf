###############################################################################
# Azure Group
###############################################################################

module "azure_group" {
  source = "../_modules/data/azure_group"

  prefix    = local.prefix
  env_short = local.env_short
}


###############################################################################
# key_vault
###############################################################################
module "key_vault" {
  source = "../_modules/key_vault"

  project   = "${local.prefix}-${local.env_short}-${local.app_domain}"
  prefix    = local.prefix
  env_short = local.env_short
  location  = local.location
  tags      = local.tags

  azdo_sp_tls_cert_enabled                         = local.azdo_sp_tls_cert_enabled
  azuread_service_principal_azure_cdn_frontdoor_id = "f3b3f72f-4770-47a5-8c1e-aa298003be12"
}
