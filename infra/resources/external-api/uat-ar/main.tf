
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