
###############################################################################
# Container App
###############################################################################

module "container_app_registry_proxy_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = local.container_app
  container_app_name             = local.ca_base_name
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-external-api-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings
  secrets_names                  = local.secrets_names
  workload_profile_name          = "Consumption"
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = local.probes
  tags                           = module.local.config.tags
}


###############################################################################
# APIM
###############################################################################

module "apim" {
  source = "../../_modules/apim_external_api"

  prefix                           = module.local.config.prefix
  env                              = module.local.config.env
  env_short                        = module.local.config.env_short
  location                         = module.local.config.location
  tags                             = module.local.config.tags
  location_short                   = module.local.config.location_short
  domain                           = module.local.config.domain
  dns_zone_prefix                  = module.local.config.dns_zone_prefix
  external_domain                  = module.local.config.external_domain
  ca_suffix_dns_private_name       = module.local.config.private_dns_name_domain
  apim_publisher_name              = local.apim_publisher_name
  apim_sku                         = local.apim_sku
  private_dns_name                 = local.private_dns_name
  private_onboarding_dns_name      = local.private_onboarding_dns_name
  cidr_subnet_apim                 = local.cidr_subnet_apim
  app_gateway_api_certificate_name = local.app_gateway_api_certificate_name
  ca_pnpg_suffix_dns_private_name  = local.ca_pnpg_suffix_dns_private_name
  developer_path                   = "${path.module}/developer"
}