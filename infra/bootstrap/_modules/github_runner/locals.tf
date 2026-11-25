locals {
  project = "${var.prefix}-${var.env_short}"

  key_vault_naming = {
    prefix          = var.prefix
    environment     = var.env_short
    location        = var.location
    domain          = "ar"
    instance_number = "01"
  }

  environment = {
    d = "dev"
    u = "uat"
    p = "prod"
  }

  # Workaround for CVE-2026-114938 (Java4Shell) - OpenJDK AVX-512 VBMI Base64 intrinsic bug
  java4shell_mitigation = {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-XX:-UseBASE64Intrinsics"
  }
}
