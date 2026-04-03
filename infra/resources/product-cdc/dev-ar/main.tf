###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-dev-ar"
}

###############################################################################
# Container App Product CDC
###############################################################################
locals {
  app_settings = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar",
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "product-cdc",
    },
    {
      name  = "MONGODB_DATABASE_NAME"
      value = "selcProduct"
    },
    {
      name  = "MONGODB_COLLECTION_NAME"
      value = "products"
    },
    {
      name  = "PRODUCT-CDC-MONGODB-WATCH-ENABLED"
      value = true
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-${module.local.config.env_short}-product"
    }
  ]

  secrets_names = {
    "BLOB_STORAGE_CONN_STRING_PRODUCT"      = "blob-storage-product-connection-string"
    "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
    "JWT_PUBLIC_KEY"                        = "jwt-public-key"
  }
}

module "container_app_product_cdc" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-product-cdc"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-product-cdc"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings
  secrets_names                  = local.secrets_names
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}
