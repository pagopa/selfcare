terraform {
  required_providers {
    restapi = {
      source  = "mastercard/restapi"
      version = "3.0.0"
    }
  }
}
provider "restapi" {
  alias                = "search"
  uri                  = "https://${var.srch_service_name}.search.windows.net"
  write_returns_object = true
  debug                = true
  insecure             = false

  headers = {
    "api-key"      = var.srch_service_primary_key,
    "Content-Type" = "application/json"
  }

  create_method  = "POST"
  update_method  = "PUT"
  destroy_method = "DELETE"
}
