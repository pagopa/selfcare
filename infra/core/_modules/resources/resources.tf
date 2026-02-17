# email templates
resource "null_resource" "upload_resources_templates" {
  triggers = {
    dir_sha1 = sha1(join("", [for f in fileset("${path.module}/resources/templates", "**") : filesha1("${path.module}/resources/templates/${f}")]))
  }
  provisioner "local-exec" {
    command = <<EOT
              az storage blob sync --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --source "./resources/templates" \
                --destination 'resources/templates/'
              az cdn endpoint purge \
                --resource-group ${var.checkout_fe_rg_name} \
                --name ${var.checkout_cdn_name} \
                --profile-name ${replace(var.checkout_cdn_name, "-cdn-endpoint", "-cdn-profile")}  \
                --content-paths "/resources/templates/*" \
                --no-wait
          EOT
  }
}


# aggregates csv templates
resource "null_resource" "upload_resources_aggregates" {
  triggers = {
    dir_sha1 = sha1(join("", [for f in fileset("${path.module}/resources/aggregates", "**") : filesha1("${path.module}/resources/aggregates/${f}")]))
  }
  provisioner "local-exec" {
    command = <<EOT
              az storage blob sync --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --source "${path.module}/resources/aggregates" \
                --destination 'resources/aggregates/'
              az cdn endpoint purge \
                --resource-group ${var.checkout_fe_rg_name} \
                --name ${var.checkout_cdn_name} \
                --profile-name ${replace(var.checkout_cdn_name, "-cdn-endpoint", "-cdn-profile")}  \
                --content-paths "/resources/aggregates/*" \
                --no-wait
          EOT
  }
}


resource "null_resource" "upload_resources_products_logo" {
  triggers = {
    dir_sha1 = sha1(join("", [for f in fileset("${path.module}/resources/products", "**") : filesha1("${path.module}/resources/products/${f}")]))
  }
  provisioner "local-exec" {
    command = <<EOT
              az storage blob sync --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --source "./resources/templates" \
                --destination 'resources/templates/'
              az cdn endpoint purge \
                --resource-group ${var.checkout_fe_rg_name} \
                --name ${var.checkout_cdn_name} \
                --profile-name ${replace(var.checkout_cdn_name, "-cdn-endpoint", "-cdn-profile")}  \
                --content-paths "/resources/products/*" \
                --no-wait
          EOT
  }
}

# default product logo
data "local_file" "resources_default_product_logo" {
  filename = "${path.module}/resources/defaultProductLogo.png"
}

resource "null_resource" "upload_resources_default_product_logo" {
  triggers = {
    "changes-in-config" : md5(data.local_file.resources_default_product_logo.content)
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --file ${data.local_file.resources_default_product_logo.filename} \
                --overwrite true \
                --name resources/products/default/logo.png
          EOT
  }
}

# default product depict-image
data "local_file" "resources_default_product_depict-image" {
  filename = "${path.module}/resources/defaultProductDepictImage.jpeg"
}

resource "null_resource" "upload_resources_default_product_resources_depict-image" {
  triggers = {
    "changes-in-config" : md5(data.local_file.resources_default_product_depict-image.content)
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --file ${data.local_file.resources_default_product_depict-image.filename} \
                --overwrite true \
                --name resources/products/default/depict-image.jpeg
          EOT
  }
}

# uploading a pagopa logo to contract storage
resource "null_resource" "upload_resources_logo" {
  triggers = {
    "changes-in-config" : md5(data.local_file.resources_default_product_logo.content)
  }
  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload --container ${var.selc_contracts_container_name} \
                --account-name ${var.selc_contracts_storage_name} \
                --account-key "${var.selc_contracts_storage_primary_access_key}" \
                --file ${data.local_file.resources_default_product_logo.filename} \
                --overwrite true \
                --name resources/logo.png
          EOT
  }
}

# anac file
data "local_file" "resources_anac_data_csv" {
  filename = "${path.module}/resources/anac/data.csv"
}

resource "null_resource" "upload_resources_anac_data_csv" {
  triggers = {
    "changes-in-config" : md5(data.local_file.resources_anac_data_csv.content)
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --file ${data.local_file.resources_anac_data_csv.filename} \
                --overwrite true \
                --name anac-data.csv
          EOT
  }
}

# ivass file
data "local_file" "resources_ivass_data_csv" {
  filename = "${path.module}/resources/ivass/data.csv"
}

resource "null_resource" "upload_resources_ivass_data_csv" {
  triggers = {
    "changes-in-config" : md5(data.local_file.resources_ivass_data_csv.content)
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --file ${data.local_file.resources_ivass_data_csv.filename} \
                --overwrite true \
                --name ivass-data.csv
          EOT
  }
}

# metadata agid spid
resource "null_resource" "upload_metadata" {
  triggers = {
    file_sha1 = filesha1("./env/${var.env}/spid/metadata.xml")
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload \
                --container '$web' \
                --account-name ${replace(replace(var.checkout_cdn_name, "-cdn-endpoint", "-sa"), "-", "")} \
                --account-key ${var.checkout_cdn_storage_primary_access_key} \
                --file "./env/${var.env}/spid/metadata.xml" \
                --overwrite true \
                --name 'spid/metadata.xml' &&
              az cdn endpoint purge \
                --resource-group ${var.checkout_fe_rg_name} \
                --name ${var.checkout_cdn_name} \
                --profile-name ${replace(var.checkout_cdn_name, "-cdn-endpoint", "-cdn-profile")}  \
                --content-paths "/spid/metadata.xml" \
                --no-wait
          EOT
  }
}

resource "null_resource" "upload_contract_templates" {
  triggers = {
    dir_sha1 = sha1(join("", [for f in fileset("${path.module}/contracts_template/contracts/template", "**") : filesha1("${path.module}/contracts_template/contracts/template/${f}")]))
  }
  provisioner "local-exec" {
    command = <<EOT
              az storage copy \
                --account-name "sc${var.env_short}${var.location_short}ardocumentsst01" \
                --connection-string "${data.azurerm_key_vault_secret.selc_documents_storage_connection_string.value}" \
                --destination-container "sc-${var.env_short}-documents-blob" \
                --source "./contracts_template/*" \
                --recursive
          EOT
  }
}
