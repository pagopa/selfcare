module "roles" {
  source  = "pagopa-dx/azure-role-assignments/azurerm"
  version = "~> 3.0"

  principal_id    = data.azurerm_user_assigned_identity.container_app_environment.principal_id
  subscription_id = var.subscription_id

  storage_queue = [
    {
      storage_account_name = module.storage_account.name
      resource_group_name  = var.resource_group_name
      queue_name           = var.queue_name
      role                 = "writer"
      description          = "Allows webhook to publish delivery notifications."
    },
    {
      storage_account_name = module.storage_account.name
      resource_group_name  = var.resource_group_name
      queue_name           = var.queue_name
      role                 = "reader"
      description          = "Allows webhook to consume delivery notifications."
    }
  ]
}
