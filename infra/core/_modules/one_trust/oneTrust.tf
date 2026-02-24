resource "null_resource" "upload_one_trust" {
  triggers = {
    dir_sha1 = sha1(join("", [for f in fileset(format("../%s/oneTrust", var.env), "**") : filesha1("../${var.env}/oneTrust/${f}")]))
  }
  provisioner "local-exec" {
    command = <<EOT
              az storage blob sync \
                --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --source "${path.module}/../../${var.env}/oneTrust" \
                --destination 'ot/' 
               az afd endpoint purge \
                --resource-group ${var.checkout_fe_rg_name} \
                --endpoint-name ${replace(var.checkout_endpoint_name, "-afd", "-fde")}  \
                --profile-name ${replace(var.checkout_endpoint_name, "-cdn-endpoint", "-cdn-profile")}  \
                --content-paths "/ot/*" \
                --no-wait
          EOT
  }
}
