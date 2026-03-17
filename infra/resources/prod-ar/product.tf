###############################################################################
# Container App
###############################################################################

locals {
  app_settings_product_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "product-ms"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "PRODUCT_MS_RETRY_MIN_BACKOFF"
      value = 5
    },
    {
      name  = "PRODUCT_MS_RETRY_MAX_BACKOFF"
      value = 60
    },
    {
      name  = "PRODUCT_MS_RETRY"
      value = 3
    },
    {
      name  = "MONGODB_DATABASE_NAME"
      value = "selcProduct"
    },
    {
      name  = "BLOB_STORAGE_CONTAINER_CONTRACT_TEMPLATE"
      value = "sc-p-documents-blob"
    }
  ]

  secrets_names_product_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"            = "appinsights-connection-string"
    "SELFCARE_DATA_ENCRIPTION_KEY"                     = "selfcare-data-encryption-key"
    "SELFCARE_DATA_ENCRIPTION_IV"                      = "selfcare-data-encryption-iv"
    "MONGODB_CONNECTION_STRING"                        = "mongodb-connection-string"
    "JWT_PUBLIC_KEY"                                   = "jwt-public-key"
    "BLOB_STORAGE_CONNECTION_STRING_CONTRACT_TEMPLATE" = "documents-storage-connection-string"
  }

  app_settings_product_cdc = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "product-cdc"
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
      value = "false"
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-p-product"
    }
  ]

  secrets_names_product_cdc = {
    "BLOB_STORAGE_CONN_STRING_PRODUCT"      = "blob-storage-product-connection-string"
    "STORAGE_CONNECTION_STRING"             = "blob-storage-product-connection-string"
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
    "JWT_PUBLIC_KEY"                        = "jwt-public-key"
  }
}

module "container_app_product_ms" {
  source = "../_modules/container_app_microservice"

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.microservice_container_app
  container_app_name             = "${local.project}-product-ms"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-product-ms"
  image_tag                      = var.product_image_tag
  app_settings                   = local.app_settings_product_ms
  secrets_names                  = local.secrets_names_product_ms

  key_vault_resource_group_name = local.key_vault_resource_group_name
  key_vault_name                = local.key_vault_name

  probes = local.quarkus_health_probes

  tags = local.tags
}

module "container_app_product_cdc" {
  source = "../_modules/container_app_microservice"

  env_short                      = local.env_short
  resource_group_name            = local.ca_resource_group_name
  container_app                  = local.microservice_container_app
  container_app_name             = "${local.project}-product-cdc"
  container_app_environment_name = local.container_app_environment_name
  image_name                     = "selfcare-product-cdc"
  image_tag                      = var.product_cdc_image_tag
  app_settings                   = local.app_settings_product_cdc
  secrets_names                  = local.secrets_names_product_cdc

  key_vault_resource_group_name = local.key_vault_resource_group_name
  key_vault_name                = local.key_vault_name

  probes = local.quarkus_health_probes

  tags = local.tags
}
