module "container_app_job" {
  source              = "pagopa-dx/github-selfhosted-runner-on-container-app-jobs/azurerm"
  version             = "~> 3.0"
  resource_group_name = azurerm_resource_group.rg_github_runner.name

  environment = {
    prefix          = var.prefix
    env_short       = var.env_short
    location        = var.location
    instance_number = "01"
  }

  repository = {
    owner = "pagopa"
    name  = "selfcare"
  }

  container_app_environment = {
    id              = module.container_app_environment_runner.id
    location        = var.location
    cpu             = var.container.cpu
    memory          = var.container.memory
    image           = var.container.image
    min_instances   = 0
    max_instances   = 5
    use_labels      = true
    override_labels = [local.environment[var.env_short]]
  }

  key_vault = {
    name                = data.azurerm_key_vault.key_vault_common.name
    resource_group_name = data.azurerm_key_vault.key_vault_common.resource_group_name
    use_rbac            = data.azurerm_key_vault.key_vault_common.enable_rbac_authorization
  }

  use_github_app = true

  tags = var.tags
}
