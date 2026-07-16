module "roles" {
  source  = "pagopa-dx/azure-role-assignments/azurerm"
  version = "~> 3.0"

  principal_id    = data.azurerm_user_assigned_identity.container_app_environment.principal_id
  subscription_id = var.subscription_id

  service_bus = [
    {
      namespace_name      = local.namespace.name
      resource_group_name = var.resource_group_name
      queue_names         = [azurerm_servicebus_queue.this.name]
      role                = "writer"
      description         = "Allows webhook to publish delivery notifications."
    },
    {
      namespace_name      = local.namespace.name
      resource_group_name = var.resource_group_name
      queue_names         = [azurerm_servicebus_queue.this.name]
      role                = "reader"
      description         = "Allows webhook to consume delivery notifications."
    }
  ]
}
