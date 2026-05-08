# locals {
#   resource_groups_name   = local.rg_apim_name
#   service_name           = data.azurerm_api_management.apim.name
#   azure_apim_api_version = "2021-08-01"
#   checkout_cdn_name      = "${local.project}-checkout-cdn-endpoint"
#   checkout_cdn_name_pnpg = "${local.project}-weu-pnpg-checkout-cdn-endpoint"
# }

# resource "null_resource" "upload_developer_index_v2" {
#   triggers = {
#     file_sha1 = filesha1("${var.developer_path}/external/v2/index.html")
#   }

#   provisioner "local-exec" {
#     command = <<EOT
#               az storage blob upload \
#                 --container '$web' \
#                 --account-name ${replace(replace(local.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
#                 --account-key ${data.azurerm_storage_account.checkout.primary_access_key} \
#                 --file "${path.module}/developer/external/v2/index.html" \
#                 --overwrite true \
#                 --name 'developer/external/v2/index.html' &&
#               az cdn endpoint purge \
#                 --resource-group ${data.azurerm_storage_account.checkout.resource_group_name} \
#                 --name ${local.checkout_cdn_name} \
#                 --profile-name ${replace(local.checkout_cdn_name, "-cdn-endpoint", "-cdn-profile")}  \
#                 --content-paths "/developer/external/v2/index.html" \
#                 --no-wait
#           EOT
#   }
# }

# resource "null_resource" "upload_support_developer_index_v1" {
#   triggers = {
#     file_sha1 = filesha1("${var.developer_path}/support/v1/index.html")
#   }

#   provisioner "local-exec" {
#     command = <<EOT
#               az storage blob upload \
#                 --container '$web' \
#                 --account-name ${replace(replace(local.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
#                 --account-key ${data.azurerm_storage_account.checkout.primary_access_key} \
#                 --file "${path.module}/developer/support/v1/index.html" \
#                 --overwrite true \
#                 --name 'developer/support/v1/index.html' &&
#               az cdn endpoint purge \
#                 --resource-group ${data.azurerm_storage_account.checkout.resource_group_name} \
#                 --name ${local.checkout_cdn_name} \
#                 --profile-name ${replace(local.checkout_cdn_name, "-cdn-endpoint", "-cdn-profile")}  \
#                 --content-paths "/developer/support/v1/index.html" \
#                 --no-wait
#           EOT
#   }
# }

# resource "null_resource" "upload_internal_developer_index_v1" {
#   triggers = {
#     file_sha1 = filesha1("${var.developer_path}/internal/v1/index.html")
#   }

#   provisioner "local-exec" {
#     command = <<EOT
#               az storage blob upload \
#                 --container '$web' \
#                 --account-name ${replace(replace(local.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
#                 --account-key ${data.azurerm_storage_account.checkout.primary_access_key} \
#                 --file "${path.module}/developer/internal/v1/index.html" \
#                 --overwrite true \
#                 --name 'developer/internal/v1/index.html' &&
#               az cdn endpoint purge \
#                 --resource-group ${data.azurerm_storage_account.checkout.resource_group_name} \
#                 --name ${local.checkout_cdn_name} \
#                 --profile-name ${replace(local.checkout_cdn_name, "-cdn-endpoint", "-cdn-profile")}  \
#                 --content-paths "/developer/internal/v1/index.html" \
#                 --no-wait
#           EOT
#   }
# }

# resource "null_resource" "upload_billing_developer_index_v1" {
#   triggers = {
#     file_sha1 = filesha1("${var.developer_path}/billing-portal/v1/index.html")
#   }

#   provisioner "local-exec" {
#     command = <<EOT
#               az storage blob upload \
#                 --container '$web' \
#                 --account-name ${replace(replace(local.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
#                 --account-key ${data.azurerm_storage_account.checkout.primary_access_key} \
#                 --file "${path.module}/developer/billing-portal/v1/index.html" \
#                 --overwrite true \
#                 --name 'developer/billing-portal/v1/index.html' &&
#               az cdn endpoint purge \
#                 --resource-group ${data.azurerm_storage_account.checkout.resource_group_name} \
#                 --name ${local.checkout_cdn_name} \
#                 --profile-name ${replace(local.checkout_cdn_name, "-cdn-endpoint", "-cdn-profile")}  \
#                 --content-paths "/developer/billing-portal/v1/index.html" \
#                 --no-wait
#           EOT
#   }
# }

# resource "null_resource" "upload_support_pnpg_developer_index_v1" {
#   triggers = {
#     file_sha1 = filesha1("${var.developer_path}/support-pnpg/v1/index.html")
#   }

#   provisioner "local-exec" {
#     command = <<EOT
#               az storage blob upload \
#                 --container '$web' \
#                 --account-name ${replace(replace(local.checkout_cdn_name_pnpg, "-cdn-endpoint", "-sa"), "-", "")} \
#                 --account-key ${data.azurerm_storage_account.checkout_pnpg.primary_access_key} \
#                 --file "${path.module}/developer/support-pnpg/v1/index.html" \
#                 --overwrite true \
#                 --name 'developer/support-pnpg/v1/index.html' &&
#               az cdn endpoint purge \
#                 --resource-group ${data.azurerm_storage_account.checkout_pnpg.resource_group_name} \
#                 --name ${local.checkout_cdn_name_pnpg} \
#                 --profile-name ${replace(local.checkout_cdn_name_pnpg, "-cdn-endpoint", "-cdn-profile")}  \
#                 --content-paths "/developer/support-pnpg/v1/index.html" \
#                 --no-wait
#           EOT
#   }
# }

# output "developer_path" {
#   value = "${var.developer_path}"
# }