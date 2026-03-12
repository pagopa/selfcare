###############################################################################
# Storage Account Template v4 ****** Should not be used ******
###############################################################################

resource "azurerm_resource_group" "this" {
  name     = "${var.project}-${var.name}-storage-rg"
  location = var.location
  tags     = var.tags
}

# tfsec:ignore:azure-storage-default-action-deny
module "storage_account" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//storage_account?ref=v6.6.0"

  name                            = replace(var.storage_account_name, "-", "")
  account_kind                    = "StorageV2"
  account_tier                    = "Standard"
  account_replication_type        = var.account_replication_type
  access_tier                     = "Hot"
  blob_versioning_enabled         = var.enable_versioning
  resource_group_name             = azurerm_resource_group.this.name
  location                        = var.location
  advanced_threat_protection      = var.advanced_threat_protection
  allow_nested_items_to_be_public = false
  public_network_access_enabled   = var.public_network_access_enabled
  blob_delete_retention_days      = var.delete_retention_days

  tags = var.tags
}

resource "azurerm_management_lock" "this" {
  count      = var.enable_management_lock ? 1 : 0
  name       = "${var.name}-storage-blob-container-lock"
  scope      = module.storage_account.id
  lock_level = "CanNotDelete"
  notes      = "This items can't be deleted in this subscription!"
}

# tfsec:ignore:AZU023
resource "azurerm_key_vault_secret" "access_key" {
  name         = "${var.name}-storage-access-key"
  value        = module.storage_account.primary_access_key
  content_type = "text/plain"

  key_vault_id = var.key_vault_id
}

# tfsec:ignore:azure-keyvault-ensure-secret-expiry
resource "azurerm_key_vault_secret" "connection_string" {
  name         = "${var.name}-storage-connection-string"
  value        = module.storage_account.primary_connection_string
  content_type = "text/plain"

  key_vault_id = var.key_vault_id
}

# tfsec:ignore:azure-keyvault-ensure-secret-expiry
resource "azurerm_key_vault_secret" "blob_connection_string" {
  name         = "${var.name}-storage-blob-connection-string"
  value        = module.storage_account.primary_blob_connection_string
  content_type = "text/plain"

  key_vault_id = var.key_vault_id
}

resource "azurerm_storage_container" "this" {
  name                  = "${var.project}-${var.name}-blob"
  storage_account_name  = module.storage_account.name
  container_access_type = "private"
}

module "subnet" {
  source                            = "github.com/pagopa/terraform-azurerm-v4.git//subnet?ref=v6.6.0"
  name                              = "${var.project}-${var.name}-storage-snet"
  address_prefixes                  = var.cidr_subnet
  resource_group_name               = var.rg_vnet_name
  virtual_network_name              = var.vnet_name
  private_endpoint_network_policies = var.private_endpoint_network_policies

  service_endpoints = [
    "Microsoft.Storage",
  ]
}

resource "azurerm_private_endpoint" "this" {
  name                = "${var.project}-${var.name}_storage"
  location            = var.location
  resource_group_name = azurerm_resource_group.this.name
  subnet_id           = module.subnet.id

  private_service_connection {
    name                           = "${var.project}-${var.name}_storage-private-endpoint"
    private_connection_resource_id = module.storage_account.id
    is_manual_connection           = false
    subresource_names              = ["Blob"]
  }

  private_dns_zone_group {
    name                 = "private-dns-zone-group"
    private_dns_zone_ids = var.private_dns_zone_ids
  }
}

