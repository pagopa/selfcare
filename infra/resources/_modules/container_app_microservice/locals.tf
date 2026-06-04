locals {
  project = "selc-${var.env_short}"

  resource_group_name                     = var.resource_group_name
  monitor_resource_group_name             = "${local.project}-monitor-rg"
  vnet_name                               = "${local.project}-vnet-rg"
  key_vault_resource_group_name           = var.key_vault_resource_group_name
  key_vault_name                          = var.key_vault_name
  app_name                                = "${var.container_app_name}-ca"
  restart_alert_enabled                   = var.restart_alert.enabled && var.env_short != "d"
  restart_alert_name                      = "${var.container_app_name}-restart-alert"
  restart_alert_action_group_name         = var.restart_alert.action_group_name
  restart_alert_action_group_rg_name      = coalesce(var.restart_alert.action_group_rg_name, local.monitor_resource_group_name)
  container_app_environment_dns_zone_name = "azurecontainerapps.io"
  # Defensive sanitation: DX reusable workflows can pass escaped suffixes after sha.
  # Remove "\" and, for sha tags, keep only "sha-" + 7 chars.
  cleaned_image_tag   = replace(trimspace(var.image_tag), "\\", "")
  sanitized_image_tag = length(local.cleaned_image_tag) == 0 ? "latest" : (startswith(local.cleaned_image_tag, "sha-") && length(local.cleaned_image_tag) > 11 ? substr(local.cleaned_image_tag, 0, 11) : local.cleaned_image_tag)

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
