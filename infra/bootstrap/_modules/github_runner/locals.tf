locals {
  project = "${var.prefix}-${var.env_short}"

  key_vault_naming = {
    prefix          = var.prefix
    environment     = var.env_short
    location        = var.location
    domain          = "ar"
    instance_number = "01"
  }

  environment = {
    d = "dev"
    u = "uat"
    p = "prod"
  }
}
