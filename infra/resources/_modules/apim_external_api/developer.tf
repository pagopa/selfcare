locals {
  resource_groups_name   = local.rg_apim_name
  service_name           = data.azurerm_api_management.apim.name
  azure_apim_api_version = "2021-08-01"
  checkout_cdn_name      = "${local.project}-checkout-cdn-endpoint"
  checkout_cdn_name_pnpg = "${local.project}-weu-pnpg-checkout-cdn-endpoint"
  profile_name           = "${local.project}-${var.location_short}-ar-checkout-afd-01"
  endpoint_name          = "${local.project}-${var.location_short}-ar-checkout-fde-01"
}

resource "null_resource" "upload_developer_index_v2" {
  triggers = {
    file_sha1 = filesha1("${path.module}/../../api/${var.env}-ar/developer/external/v2/index.html")
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload \
                --container '$web' \
                --account-name ${replace(data.azurerm_storage_account.checkout.name, "-", "")} \
                --account-key ${data.azurerm_storage_account.checkout.primary_access_key} \
                --file "${path.module}/../../external-api/${var.env}-ar/developer/external/v2/index.html" \
                --overwrite true \
                --name 'developer/external/v2/index.html' &&
              az afd endpoint purge \
                --resource-group ${data.azurerm_storage_account.checkout.resource_group_name} \
                --endpoint-name "${local.endpoint_name}" \
                --profile-name "${local.profile_name}" \
                --content-paths "/developer/external/v2/index.html" \
                --no-wait
          EOT
  }
}

resource "null_resource" "upload_support_developer_index_v1" {
  triggers = {
    file_sha1 = filesha1("${path.module}/../../api/${var.env}-ar/developer/support/v1/index.html")
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload \
                --container '$web' \
                --account-name ${replace(data.azurerm_storage_account.checkout.name, "-", "")} \
                --account-key ${data.azurerm_storage_account.checkout.primary_access_key} \
                --file "${path.module}/../../external-api/${var.env}-ar/developer/support/v1/index.html" \
                --overwrite true \
                --name 'developer/support/v1/index.html' &&
              az afd endpoint purge \
                --resource-group ${data.azurerm_storage_account.checkout.resource_group_name} \
                --endpoint-name "${local.endpoint_name}" \
                --profile-name "${local.profile_name}" \
                --content-paths "/developer/support/v1/index.html" \
                --no-wait
          EOT
  }
}

resource "null_resource" "upload_internal_developer_index_v1" {
  triggers = {
    file_sha1 = filesha1("${path.module}/../../api/${var.env}-ar/developer/internal/v1/index.html")
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload \
                --container '$web' \
                --account-name ${replace(data.azurerm_storage_account.checkout.name, "-", "")} \
                --account-key ${data.azurerm_storage_account.checkout.primary_access_key} \
                --file "${path.module}/../../external-api/${var.env}-ar/developer/internal/v1/index.html" \
                --overwrite true \
                --name 'developer/internal/v1/index.html' &&
              az afd endpoint purge \
                --resource-group ${data.azurerm_storage_account.checkout.resource_group_name} \
                --endpoint-name "${local.endpoint_name}" \
                --profile-name "${local.profile_name}" \
                --content-paths "/developer/internal/v1/index.html" \
                --no-wait
          EOT
  }
}

resource "null_resource" "upload_billing_developer_index_v1" {
  triggers = {
    file_sha1 = filesha1("${path.module}/../../api/${var.env}-ar/developer/billing-portal/v1/index.html")
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload \
                --container '$web' \
                --account-name ${replace(data.azurerm_storage_account.checkout.name, "-", "")} \
                --account-key ${data.azurerm_storage_account.checkout.primary_access_key} \
                --file "${path.module}/../../external-api/${var.env}-ar/developer/billing-portal/v1/index.html" \
                --overwrite true \
                --name 'developer/billing-portal/v1/index.html' &&
              az afd endpoint purge \
                --resource-group ${data.azurerm_storage_account.checkout.resource_group_name} \
                --endpoint-name "${local.endpoint_name}" \
                --profile-name "${local.profile_name}" \
                --content-paths "/developer/billing-portal/v1/index.html" \
                --no-wait
          EOT
  }
}

resource "null_resource" "upload_support_pnpg_developer_index_v1" {
  triggers = {
    file_sha1 = filesha1("${path.module}/../../api/${var.env}-ar/developer/support-pnpg/v1/index.html")
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload \
                --container '$web' \
                --account-name ${replace(data.azurerm_storage_account.checkout.name, "-", "")} \
                --account-key ${data.azurerm_storage_account.checkout.primary_access_key} \
                --file "${path.module}/../../external-api/${var.env}-ar/developer/support-pnpg/v1/index.html" \
                --overwrite true \
                --name 'developer/support-pnpg/v1/index.html' &&
              az afd endpoint purge \
                --resource-group ${data.azurerm_storage_account.checkout.resource_group_name} \
                --endpoint-name "${local.endpoint_name}" \
                --profile-name "${local.profile_name}" \
                --content-paths "/developer/support-pnpg/v1/index.html" \
                --no-wait
          EOT
  }
}
