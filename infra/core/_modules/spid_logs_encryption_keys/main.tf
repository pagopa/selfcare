module "spid_logs_encryption_keys" {
  source = "github.com/pagopa/terraform-azurerm-v4.git//jwt_keys?ref=v9.6.1"

  jwt_name         = "spid-logs-encryption"
  key_vault_id     = var.key_vault_id
  cert_common_name = "spid-logs"
  cert_password    = ""
  tags             = var.tags
  # cert_allowed_uses = ["crl_signing", "data_encipherment", "digital_signature", "key_agreement", "cert_signing", "key_encipherment"]
}