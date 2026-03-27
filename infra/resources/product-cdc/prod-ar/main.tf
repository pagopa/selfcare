###############################################################################
# Container App Product CDC
###############################################################################

module "container_app_product_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.product_cdc_container_app
  container_app_name             = "${local.project}-product-cdc"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-product-cdc"
  image_tag                      = local.product_cdc_image_tag
  app_settings                   = local.app_settings_product_cdc
  secrets_names                  = local.secrets_names_product_cdc

  key_vault_resource_group_name = local.key_vault_resource_group_name
  key_vault_name                = local.key_vault_name

  probes = local.quarkus_health_probes

  tags = local.tags
}
