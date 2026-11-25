locals {
  project = "selc-${var.env_short}"

  resource_group_name           = var.resource_group_name
  key_vault_resource_group_name = var.key_vault_resource_group_name
  key_vault_name                = var.key_vault_name
  app_name                      = "${var.container_app_name}-job"
  user_assigned_identity_ids = concat(
    var.environment_identity_enabled ? [data.azurerm_user_assigned_identity.cae_identity[0].id] : [],
    var.additional_user_assigned_identity_ids
  )
  identity_type = length(local.user_assigned_identity_ids) > 0 ? "SystemAssigned, UserAssigned" : "SystemAssigned"

  secrets = [for secret in var.secrets_names :
    {
      identity              = data.azurerm_user_assigned_identity.cae_identity[0].id
      name                  = secret
      key_vault_secret_name = "https://${data.azurerm_key_vault.key_vault.name}.vault.azure.net/secrets/${secret}"
  }]

  secrets_env = [for env, secret in var.secrets_names :
    {
      name      = env
      secretRef = secret
  }]

  # Workaround for CVE-2026-114938 (Java4Shell) - OpenJDK AVX-512 VBMI Base64 intrinsic bug
  java4shell_mitigation = {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-XX:-UseBASE64Intrinsics"
  }

  has_java_tool_options = anytrue([for s in var.app_settings : s.name == local.java4shell_mitigation.name])

  app_settings = !var.enable_java4shell_mitigation ? var.app_settings : concat(
    [
      for s in var.app_settings : s.name == local.java4shell_mitigation.name ? merge(s, {
        value = strcontains(coalesce(s.value, ""), local.java4shell_mitigation.value) ? s.value : trimspace("${coalesce(s.value, "")} ${local.java4shell_mitigation.value}")
      }) : s
    ],
    local.has_java_tool_options ? [] : [
      {
        name                  = local.java4shell_mitigation.name
        value                 = local.java4shell_mitigation.value
        key_vault_secret_name = tostring(null)
      }
    ]
  )

}
