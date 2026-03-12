# tfsec:ignore:azure-storage-default-action-deny
# tfsec:ignore:azure-storage-queue-services-logging-enabled
resource "azurerm_storage_account" "namirial_sws_storage_account" {
  count                           = var.enable_sws ? 1 : 0
  name                            = replace(format("%s-namirial-sws-st", local.project), "-", "")
  resource_group_name             = data.azurerm_resource_group.rg_contracts_storage.name
  location                        = data.azurerm_resource_group.rg_contracts_storage.location
  min_tls_version                 = "TLS1_2"
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  allow_nested_items_to_be_public = false
  tags                            = var.tags
}

resource "azurerm_storage_share" "namirial_sws_storage_share" {
  count = var.enable_sws ? 1 : 0
  name  = "${local.project}-namirial-sws-share"

  storage_account_name = azurerm_storage_account.namirial_sws_storage_account[0].name

  quota = 1
}