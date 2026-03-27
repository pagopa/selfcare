###############################################################################
# Onboarding BFF
###############################################################################

module "container_app_onboarding_bff_pnpg" {
  source = "../../_modules/container_app_microservice"

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.container_app_onboarding_bff
  container_app_name             = "selc-${local.env_short}-pnpg-onboarding-bff"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-onboarding-bff"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_onboarding_bff
  secrets_names                  = local.secrets_names_onboarding_bff
  key_vault_resource_group_name  = local.key_vault_resource_group_name
  key_vault_name                 = local.key_vault_name
  probes                         = local.quarkus_health_probes
  tags                           = local.tags
}

###############################################################################
# APIM
###############################################################################

module "apim_api_bff_onboarding_pnpg" {
  source              = "../../_modules/apim_api"
  apim_name           = local.apim_name_onboarding_bff
  apim_rg             = local.apim_rg_onboarding_bff
  api_name            = "selc-${local.env_short}-pnpg-api-bff-onboarding"
  display_name        = "BFF PNPG Onboarding API"
  base_path           = "imprese/onboarding"
  private_dns_name    = local.private_dns_name_onboarding_bff
  dns_zone_prefix     = local.dns_zone_prefix_onboarding_bff
  api_dns_zone_prefix = local.api_dns_zone_prefix_onboarding_bff
  external_domain     = "pagopa.it"
  openapi_path        = "../../../apps/onboarding-bff/app/src/main/resources/swagger/api-docs.json"
}
