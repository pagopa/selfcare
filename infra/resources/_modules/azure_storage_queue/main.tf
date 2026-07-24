# radar: assess -- explicitly approved for webhook delivery reliability.
# The DX module owns the storage account, private endpoint, private DNS
# registration, and diagnostics configuration.
module "storage_account" {
  source  = "pagopa-dx/azure-storage-account/azurerm"
  version = "~> 3.0"

  environment = {
    prefix          = var.environment.prefix
    env_short       = var.environment.env_short
    location        = var.environment.location
    domain          = var.environment.domain
    app_name        = var.environment.app_name
    instance_number = var.environment.instance_number
  }

  resource_group_name = var.resource_group_name

  # Public access is disabled; the queue is reached through the private endpoint.
  subnet_pep_id                        = data.azurerm_subnet.private_endpoints.id
  private_dns_zone_resource_group_name = var.private_dns_zone_resource_group_name

  subservices_enabled = {
    queue = true
  }

  queues = [var.queue_name]

  diagnostic_settings = {
    enabled                    = true
    log_analytics_workspace_id = data.azurerm_log_analytics_workspace.monitoring.id
  }

  tags = var.tags
}
