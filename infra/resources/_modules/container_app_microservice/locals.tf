locals {
  project = "selc-${var.env_short}"

  resource_group_name                     = var.resource_group_name
  monitor_resource_group_name             = "${local.project}-monitor-rg"
  vnet_name                               = "${local.project}-vnet-rg"
  key_vault_resource_group_name           = var.key_vault_resource_group_name
  key_vault_name                          = var.key_vault_name
  app_name                                = "${var.container_app_name}-ca"
  restart_alert_enabled                   = var.restart_alert.enabled
  restart_alert_name                      = "${var.container_app_name}-restart-alert"
  restart_alert_action_group_name         = var.restart_alert.action_group_name
  restart_alert_action_group_rg_name      = coalesce(var.restart_alert.action_group_rg_name, local.monitor_resource_group_name)
  container_app_environment_dns_zone_name = "azurecontainerapps.io"
  # Defensive sanitation: DX reusable workflows can pass escaped suffixes after sha.
  # Remove "\" and, for sha tags, keep only "sha-" + 7 chars.
  cleaned_image_tag   = replace(trimspace(var.image_tag), "\\", "")
  sanitized_image_tag = length(local.cleaned_image_tag) == 0 ? "latest" : (startswith(local.cleaned_image_tag, "sha-") && length(local.cleaned_image_tag) > 11 ? substr(local.cleaned_image_tag, 0, 11) : local.cleaned_image_tag)

  # Multitenant data-layer registry (apps/docs/Multitenant/Step_1/EPIC.md sub-task 9) delivered to
  # every microservice through one environment variable, so that services consume the single
  # Terraform registry (module.local.config.tenant_data_isolation) instead of each stack declaring
  # its own per-tenant names. Omitted entirely when unset, leaving the application on its
  # fail-closed default rather than an empty-string registry.
  tenant_data_isolation_env = var.tenant_data_isolation_json == null ? [] : [{
    name                  = "SELFCARE_TENANT_DATA_ISOLATION"
    value                 = var.tenant_data_isolation_json
    key_vault_secret_name = null
  }]

  # Migration switch for the tenant discriminator (Step_1 sub-tasks 2 and 10). Kept out of the env
  # block entirely when unset, so an unwired stack keeps the application default instead of being
  # pinned to "false" from Terraform: the difference matters when the flag is eventually deleted.
  strict_tenant_data_isolation_env = var.strict_tenant_data_isolation == null ? [] : [{
    name                  = "SELFCARE_TENANT_STRICT_DATA_ISOLATION"
    value                 = tostring(var.strict_tenant_data_isolation)
    key_vault_secret_name = null
  }]

  app_settings = concat(var.app_settings, local.tenant_data_isolation_env, local.strict_tenant_data_isolation_env)

  secrets = [for secret in var.secrets_names :
    {
      identity              = data.azurerm_user_assigned_identity.cae_identity.id
      name                  = secret
      key_vault_secret_name = "https://${data.azurerm_key_vault.key_vault.name}.vault.azure.net/secrets/${secret}"
  }]

  secrets_env = [for env, secret in var.secrets_names :
    {
      name      = env
      secretRef = secret
  }]

}
