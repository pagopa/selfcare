# # Role assignment per Event Hub
# resource "azurerm_role_assignment" "eventhub_data_receiver" {
#   scope                = azurerm_eventhub_namespace.main.id
#   role_definition_name = "Azure Event Hubs Data Receiver"
#   principal_id         = data.azurerm_user_assigned_identity.cae_identity.principal_id
# }

# # Role assignment per Search Service
# resource "azurerm_role_assignment" "search_contributor" {
#   scope                = data.azurerm_search_service.main.id
#   role_definition_name = "Search Service Contributor"
#   principal_id         = data.azurerm_user_assigned_identity.cae_identity.principal_id
# }