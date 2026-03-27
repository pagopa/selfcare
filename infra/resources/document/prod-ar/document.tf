###############################################################################
# Container App
###############################################################################

locals {
  app_settings_document_ms = [
    {
      name  = "JAVA_TOOL_OPTIONS"
      value = "-javaagent:applicationinsights-agent.jar"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "document-ms"
    },
    {
      name  = "STORAGE_CONTAINER_PRODUCT"
      value = "selc-p-product"
    },
    {
      name  = "STORAGE_CONTAINER_CONTRACT"
      value = "sc-p-documents-blob"
    }
  ]

  secrets_names_document_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
    "JWT-PUBLIC-KEY"                          = "jwt-public-key"
    "MONGODB-CONNECTION-STRING"               = "mongodb-connection-string"
    "BLOB-STORAGE-PRODUCT-CONNECTION-STRING"  = "blob-storage-product-connection-string"
    "BLOB-STORAGE-CONTRACT-CONNECTION-STRING" = "documents-storage-connection-string"
  }
}

# module "container_app_document_ms" {
#   source = "../../_modules/container_app_microservice"

#   env_short                      = local.env_short
#   resource_group_name            = local.ca_resource_group_name
#   container_app                  = local.microservice_container_app
#   container_app_name             = "${local.project}-document-ms"
#   container_app_environment_name = local.container_app_environment_name
#   image_name                     = "selfcare-document-ms"
#   image_tag                      = local.document_image_tag
#   app_settings                   = local.app_settings_document_ms
#   secrets_names                  = local.secrets_names_document_ms

#   key_vault_resource_group_name = local.key_vault_resource_group_name
#   key_vault_name                = local.key_vault_name

#   probes = local.quarkus_health_probes

#   tags = local.tags
# }
