resource "null_resource" "upload_assets" {
  triggers = {
    dir_sha1 = sha1(join("", [for f in fileset("${path.module}/../../assets", "**") : filesha1("${path.module}/../../assets/${f}")]))
  }
  provisioner "local-exec" {
    command = <<EOT
              az storage blob sync \
                --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --source "${path.module}/../../assets" \
                --destination 'assets/'
              az afd endpoint purge \
                  --resource-group ${var.checkout_fe_rg_name} \
                  --endpoint-name ${replace(var.checkout_endpoint_name, "-afd", "-fde")}  \
                  --profile-name ${replace(var.checkout_endpoint_name, "-cdn-endpoint", "-cdn-profile")}  \
                --content-paths "/assets/*" \
                --no-wait
          EOT
  }
}


resource "null_resource" "upload_alert_message" {
  triggers = {
    file_sha1 = filesha1("${path.module}/../../${var.env}/assets/login-alert-message.json")
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload \
                --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --file "${path.module}/../../${var.env}/assets/login-alert-message.json" \
                --overwrite true \
                --name 'assets/login-alert-message.json'

              az afd endpoint purge \
                --content-paths "/assets/login-alert-message.json" \
                --resource-group ${var.checkout_fe_rg_name} \
                --endpoint-name ${replace(var.checkout_endpoint_name, "-afd", "-fde")}  \
                --profile-name ${var.checkout_endpoint_name}  \
                --no-wait
          EOT
  }
}


resource "null_resource" "upload_spid_idp_status" {
  triggers = {
    file_sha1 = filesha1("${path.module}/../../${var.env}/assets/spid_idp_status.json")
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload \
                --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --file "${path.module}/../../${var.env}/assets/spid_idp_status.json" \
                --overwrite true \
                --name 'assets/spid_idp_status.json'

                az afd endpoint purge \
                  --content-paths "/assets/spid_idp_status.json" \
                  --resource-group ${var.checkout_fe_rg_name} \
                  --endpoint-name ${replace(var.checkout_endpoint_name, "-afd", "-fde")}  \
                  --profile-name ${var.checkout_endpoint_name}  \
                  --no-wait
          EOT
  }
}

resource "null_resource" "upload_config" {
  triggers = {
    file_sha1 = filesha1("${path.module}/../../${var.env}/assets/config.json")
  }

  provisioner "local-exec" {
    command = <<EOT
      az storage blob upload \
        --container '$web' \
        --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
        --account-key ${var.checkout_cdn_storage_primary_access_key} \
        --file "${path.module}/../../${var.env}/assets/config.json" \
        --overwrite true \
        --name 'assets/config.json'
      az afd endpoint purge \
        --content-paths "/assets/config.json" \
        --resource-group ${var.checkout_fe_rg_name} \
        --endpoint-name ${replace(var.checkout_endpoint_name, "-afd", "-fde")}  \
        --profile-name ${var.checkout_endpoint_name}  \
        --no-wait
    EOT
  }
}
