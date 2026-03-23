resource "azurerm_resource_group" "srch_rg" {
  name     = "${var.project}-srch-rg"
  location = var.location

  tags = var.tags
}
# Azure AI Search Service
resource "azurerm_search_service" "srch_service" {
  name                = "${var.project}-srch"
  resource_group_name = azurerm_resource_group.srch_rg.name
  location            = azurerm_resource_group.srch_rg.location
  sku                 = var.sku

  replica_count                 = 1
  partition_count               = 1
  public_network_access_enabled = var.public_network_access_enabled
  allowed_ips                   = []

  local_authentication_enabled = true
  authentication_failure_mode  = "http403"

  identity {
    type = "SystemAssigned"
  }

  tags = var.tags
}


resource "azurerm_key_vault_secret" "azure_srch_api_key" {
  name         = "azure-search-api-key"
  value        = azurerm_search_service.srch_service.primary_key
  key_vault_id = data.azurerm_key_vault.key_vault.id
  content_type = "text/plain"
  depends_on   = [azurerm_search_service.srch_service]
}