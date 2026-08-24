locals {
  cleaned_image_tag   = replace(trimspace(var.image_tag), "\\", "")
  sanitized_image_tag = length(local.cleaned_image_tag) == 0 ? "latest" : (startswith(local.cleaned_image_tag, "sha-") && length(local.cleaned_image_tag) > 11 ? substr(local.cleaned_image_tag, 0, 11) : local.cleaned_image_tag)
}
