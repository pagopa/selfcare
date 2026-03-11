
# default product logo
data "local_file" "resources_file" {
  filename = var.file_path
}

resource "null_resource" "upload_resources_logo" {
  triggers = {
    "changes-in-config" : md5(data.local_file.resources_file.content)
  }

  provisioner "local-exec" {
    command = <<EOT
              az storage blob upload --container '${var.container}' \
                --connection-string '${var.primary_connection_string}' \
                --file ${data.local_file.resources_file.filename} \
                --overwrite true \
                --name resources/logo.png
          EOT
  }
}