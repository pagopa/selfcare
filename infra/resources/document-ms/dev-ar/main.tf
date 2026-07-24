###############################################################################
# GLOBAL VARIABLES
###############################################################################
module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"
  domain    = "ar"

  dns_zone_prefix                = "dev.selfcare"
  api_dns_zone_prefix            = "api.dev.selfcare"
  private_dns_name_domain        = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-cae-002"
  ca_resource_group_name         = "selc-d-container-app-002-rg"
  container_app_min_replicas     = 0
  container_app_cpu              = 1.0
  container_app_memory           = "2Gi"
}


###############################################################################
# CosmosDB
###############################################################################

module "cosmosdb_document" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcDocument"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_documents" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "documents"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = "selcDocument"

  lock_enable = true

  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["onboardingId"], unique = false },
    { keys = ["rootOnboardingId"], unique = false },
    { keys = ["productId"], unique = false },
    { keys = ["type"], unique = false },
    { keys = ["attachmentName"], unique = false },
    { keys = ["createdAt"], unique = false }
  ]
}

###############################################################################
# Container App
###############################################################################

###############################################################################
# DATA SOURCES
###############################################################################
data "azurerm_storage_account" "documents_storage" {
  name                = "sc${module.local.config.env_short}${module.local.config.location_short}ardocumentsst01"
  resource_group_name = "selc-${module.local.config.env_short}-documents-storage-rg"
}

data "azurerm_storage_account" "user_attachments_storage" {
  name                = "sc${module.local.config.env_short}${module.local.config.location_short}arusrattachst01"
  resource_group_name = "selc-${module.local.config.env_short}-user-attachments-storage-rg"
}

data "azurerm_user_assigned_identity" "document_storage_blob_identity" {
  name                = "selc-${module.local.config.env_short}-${module.local.config.domain}-documents-storage-blob-managed-identity"
  resource_group_name = "selc-${module.local.config.env_short}-${module.local.config.domain}-user-managed-identity-rg"
}

resource "azurerm_role_assignment" "document_user_attachments_blob_identity_role_assignment" {
  scope                = data.azurerm_storage_account.user_attachments_storage.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = data.azurerm_user_assigned_identity.document_storage_blob_identity.principal_id
}

###############################################################################
# Private Endpoint + Private DNS integration for user-attachments storage
###############################################################################
data "azurerm_subnet" "user_attachments_pep_snet" {
  name                 = "${module.local.config.project}-user-attachments-snet"
  virtual_network_name = module.local.vnet_selc_name
  resource_group_name  = module.local.vnet_resource_group_name
}

data "azurerm_private_dns_zone" "privatelink_blob_core_windows_net" {
  name                = "privatelink.blob.core.windows.net"
  resource_group_name = module.local.vnet_resource_group_name
}

resource "azurerm_private_endpoint" "user_attachments_blob_pep" {
  name                = "sc-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-usrattach-blob-pep-01"
  location            = module.local.config.location
  resource_group_name = data.azurerm_storage_account.user_attachments_storage.resource_group_name
  subnet_id           = data.azurerm_subnet.user_attachments_pep_snet.id

  private_service_connection {
    name                           = "sc-${module.local.config.env_short}-${module.local.config.location_short}-${module.local.config.domain}-usrattach-blob-pep-01"
    private_connection_resource_id = data.azurerm_storage_account.user_attachments_storage.id
    is_manual_connection           = false
    subresource_names              = ["blob"]
  }

  private_dns_zone_group {
    name                 = "private-dns-zone-group"
    private_dns_zone_ids = [data.azurerm_private_dns_zone.privatelink_blob_core_windows_net.id]
  }

  tags = module.local.config.tags
}

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
      name  = "SIGNATURE_VALIDATION_ENABLED"
      value = "false"
    },
    {
      name  = "PAGOPA_SIGNATURE_SOURCE"
      value = "namirial"
    },
    {
      name  = "STORAGE_CONTAINER_CONTRACT"
      value = "sc-d-documents-blob"
    },
    {
      name  = "STORAGE_CONTAINER_USER"
      value = "sc-d-usrattach-blob"
    },
    {
      name  = "NAMIRIAL_BASE_URL"
      value = "https://selc-${module.local.config.env_short}-namirial-sws-ca.${module.local.config.private_dns_name_domain}"
    },
    {
      name  = "DOCUMENT_MS_UPLOAD_MAX_BODY_SIZE"
      value = "10M"
    },
    {
      name  = "AZURE_STORAGE_ACCOUNT_NAME_CONTRACTS"
      value = data.azurerm_storage_account.documents_storage.name
    },
    {
      name  = "AZURE_STORAGE_ACCOUNT_NAME_USER"
      value = data.azurerm_storage_account.user_attachments_storage.name
    },
    {
      name  = "AZURE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.document_storage_blob_identity.client_id
    }
  ]

  secrets_names_document_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING"   = "appinsights-connection-string"
    "JWT_PUBLIC_KEY"                          = "jwt-public-key"
    "MONGODB_CONNECTION_STRING"               = "mongodb-connection-string"
    "NAMIRIAL_SIGN_SERVICE_IDENTITY_USER"     = "namirial-sign-service-user"
    "NAMIRIAL_SIGN_SERVICE_IDENTITY_PASSWORD" = "namirial-sign-service-psw"
  }
}

module "container_app_document_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-document-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-document-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_document_ms
  secrets_names                  = local.secrets_names_document_ms

  key_vault_resource_group_name = module.local.config.key_vault_resource_group_name
  key_vault_name                = module.local.config.key_vault_name

  probes = module.local.config.quarkus_health_probes

  tags = module.local.config.tags

  additional_user_assigned_identity_ids = [data.azurerm_user_assigned_identity.document_storage_blob_identity.id]
}

###############################################################################
# One-off migration: `terraform import` for the pre-existing Private Endpoint
#
# The PE already exists in Azure (created by the legacy core stack) with the
# Private DNS zone group already configured. Adopt it into this state with:
#
#   SUB=1ab5e788-3b98-4c63-bd05-de0c7388c853  # DEV-SelfCare subscription id
#
#   terraform import azurerm_private_endpoint.user_attachments_blob_pep \
#     "/subscriptions/$SUB/resourceGroups/selc-d-user-attachments-storage-rg/providers/Microsoft.Network/privateEndpoints/sc-d-weu-ar-usrattach-blob-pep-01"
#
# After import, `terraform plan` should be a pure `update in-place` with only
# tag reconciliation (removal of legacy `ModuleName`/`ModuleSource`/`ModuleVersion`).
# No `must be replaced` should appear — otherwise stop and re-check.
###############################################################################
