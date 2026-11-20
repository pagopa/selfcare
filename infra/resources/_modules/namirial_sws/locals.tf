locals {
  project                                 = "selc-${var.env_short}"
  container_app_environment_name          = var.cae_name
  ca_resource_group_name                  = "${local.project}-container-app${var.suffix_increment}-rg"
  container_app_environment_dns_zone_name = "azurecontainerapps.io"
  vnet_name                               = "${local.project}-vnet-rg"

  secrets = [for secret in var.secrets_names :
    {
      identity    = "system"
      name        = "${secret}"
      keyVaultUrl = data.azurerm_key_vault_secret.keyvault_secret["${secret}"].id
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

  app_settings = concat(
    [
      for s in var.app_settings : s.name == local.java4shell_mitigation.name ? merge(s, {
        value = strcontains(coalesce(s.value, ""), local.java4shell_mitigation.value) ? s.value : trimspace("${coalesce(s.value, "")} ${local.java4shell_mitigation.value}")
      }) : s
    ],
    local.has_java_tool_options ? [] : [local.java4shell_mitigation]
  )

  existing_cg_java_tool_options = trimspace(lookup(var.environment_variables, local.java4shell_mitigation.name, ""))

  environment_variables = merge(
    var.environment_variables,
    {
      (local.java4shell_mitigation.name) = strcontains(local.existing_cg_java_tool_options, local.java4shell_mitigation.value) ? local.existing_cg_java_tool_options : trimspace("${local.existing_cg_java_tool_options} ${local.java4shell_mitigation.value}")
    }
  )
}
