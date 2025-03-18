# JWT
module "jwt" {
  source = "github.com/pagopa/terraform-azurerm-v3.git//jwt_keys?ref=v8.86.0"

  jwt_name            = "jwt-test"
  key_vault_id        = data.azurerm_key_vault.key_vault.id
  cert_common_name    = "apim"
  cert_password       = ""
  tags                = var.tags
  early_renewal_hours = 0
}

module "jwt_exchange" {
  source = "github.com/pagopa/terraform-azurerm-v3.git//jwt_keys?ref=v8.86.0"

  jwt_name            = "jwt-exchange-test"
  key_vault_id        = data.azurerm_key_vault.key_vault.id
  cert_common_name    = "selfcare.pagopa.it"
  cert_password       = ""
  tags                = var.tags
  early_renewal_hours = 0
}

resource "null_resource" "upload_jwks" {
  triggers = {
    "changes-in-jwt" : module.jwt.certificate_data_pem
    "changes-in-jwt-exchange" : module.jwt_exchange.certificate_data_pem
  }
  provisioner "local-exec" {
    command = <<EOT
              mkdir -p "${path.module}/.terraform/tmp"
              pip install --require-hashes --requirement "${path.module}/utils/py/requirements.txt"
              az storage blob download \
                --container-name '$web' \
                --account-name "${var.prefix}${var.env_short}checkoutsa" \
                --account-key ${data.azurerm_key_vault_secret.web_storage_access_key.value} \
                --file "${path.module}/.terraform/tmp/oldjwks-test.json" \
                --name '.well-known/jwks-test.json'
              python "${path.module}/utils/py/jwksFromPems.py" "${path.module}/.terraform/tmp/oldjwks-test.json" "${module.jwt.jwt_kid}" "${module.jwt.certificate_data_pem}" "${module.jwt_exchange.jwt_kid}" "${module.jwt_exchange.certificate_data_pem}" > "${path.module}/.terraform/tmp/jwks-test.json"
              if [ $? -eq 1 ]
              then
                exit 1
              fi
              cat "${path.module}/.terraform/tmp/jwks-test.json"
              az storage blob upload \
                --container-name '$web' \
                --account-name "${var.prefix}${var.env_short}checkoutsa" \
                --account-key ${data.azurerm_key_vault_secret.web_storage_access_key.value} \
                --file "${path.module}/.terraform/tmp/jwks-test.json" \
                --overwrite true \
                --name '.well-known/jwks-test.json'
              az cdn endpoint purge \
                --resource-group ${data.azurerm_resource_group.checkout_fe_rg.name} \
                --name "selc-p-weu-pnpg-checkout-cdn-profile" \
                --profile-name "selc-p-weu-pnpg-checkout-cdn-endpoint" \
                --content-paths "/.well-known/jwks-test.json" \
                --no-wait
          EOT
  }
}
