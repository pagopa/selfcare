locals {
  pnpg_suffix                    = var.is_pnpg == true ? "-pnpg" : ""
  project                        = "selc-${var.env_short}"
  location_short                 = "weu"
  domain                         = "ar"
  container_app_environment_name = "${local.project}${local.pnpg_suffix}-${var.cae_name}"
  ca_resource_group_name         = "${local.project}-container-app${var.suffix_increment}-rg"

  dapr_settings = [{
    name  = "DAPR_HTTP_PORT"
    value = "3500"
    },
    {
      name  = "DAPR_GRPC_PORT"
      value = "50001"
    },
    {
      name  = "AZURE_CLIENT_ID"
      value = data.azurerm_user_assigned_identity.cae_identity.client_id
    }
  ]

  app_settings = concat(var.app_settings, local.dapr_settings)
}