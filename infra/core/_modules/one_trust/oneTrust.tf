resource "null_resource" "upload_one_trust" {
  triggers = {
    dir_sha1 = sha1(join("", [for f in fileset(format("../%s/oneTrust", var.env), "**") : filesha1("../${var.env}/oneTrust/${f}")]))
  }
  provisioner "local-exec" {
    command = <<EOT
              az storage blob sync \
                --container '$web' \
                --account-name ${replace(replace(var.cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.cdn_storage_primary_access_key} \
                --source "../${var.env}/oneTrust" \
                --destination 'ot/' \
              && \
              az cdn endpoint purge \
                -g ${var.checkout_fe_rg_name} \
                -n ${var.cdn_name} \
                --profile-name ${replace(var.cdn_name, "-cdn-endpoint", "-cdn-profile")}  \
                --content-paths "/ot/*" \
                --no-wait
          EOT
  }
}
