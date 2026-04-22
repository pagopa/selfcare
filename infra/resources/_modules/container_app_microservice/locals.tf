locals {
  project = "selc-${var.env_short}"

  resource_group_name                     = var.resource_group_name
  vnet_name                               = "${local.project}-vnet-rg"
  key_vault_resource_group_name           = var.key_vault_resource_group_name
  key_vault_name                          = var.key_vault_name
  app_name                                = "${var.container_app_name}-ca"
  container_app_environment_dns_zone_name = "azurecontainerapps.io"
  # Defensive sanitation: DX reusable workflows can pass escaped suffixes after sha.
  # Remove "\" and, for sha tags, keep only "sha-" + 7 chars.
  cleaned_image_tag                       = replace(trimspace(var.image_tag), "\\", "")
  sanitized_image_tag                     = length(local.cleaned_image_tag) == 0 ? "latest" : (startswith(local.cleaned_image_tag, "sha-") && length(local.cleaned_image_tag) > 11 ? substr(local.cleaned_image_tag, 0, 11) : local.cleaned_image_tag)

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
