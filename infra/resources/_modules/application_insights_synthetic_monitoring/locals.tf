locals {
  cleaned_image_tag   = replace(trimspace(var.image_tag), "\\", "")
  sanitized_image_tag = length(local.cleaned_image_tag) == 0 ? "latest" : (startswith(local.cleaned_image_tag, "sha-") && length(local.cleaned_image_tag) > 11 ? substr(local.cleaned_image_tag, 0, 11) : local.cleaned_image_tag)

  # Workaround for CVE-2026-114938 (Java4Shell) - OpenJDK AVX-512 VBMI Base64 intrinsic bug
  java4shell_mitigation = {
    name  = "JAVA_TOOL_OPTIONS"
    value = "-XX:-UseBASE64Intrinsics"
  }
}
