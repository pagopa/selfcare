locals {
  prefix         = "selc"
  storage_prefix = "sc"
  env_short      = "d"
  location       = "westeurope"
  location_short = "weu"
  domain         = "pnpg"

  dns_zone_prefix     = "pnpg.dev.selfcare"
  api_dns_zone_prefix = "api-pnpg.dev.selfcare"
  external_domain     = "pagopa.it"

  apim_name      = "selc-${local.env_short}-apim-v2"
  apim_rg        = "selc-${local.env_short}-api-v2-rg"
  apim_base_path = "imprese/iam"
  pnpg_suffix    = "${local.location_short}-${local.domain}"

  project = "${local.prefix}-${local.env_short}"

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${local.env_short}-${local.pnpg_suffix}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${local.env_short}-${local.pnpg_suffix}-cosmosdb-mongodb-account"
  }

  container_app_environment_name = "${local.prefix}-${local.env_short}-${local.domain}-cae-cp"
  ca_resource_group_name         = "${local.prefix}-${local.env_short}-container-app-rg"

  private_dns_name_domain = "blackhill-644148c0.westeurope.azurecontainerapps.io"
  private_dns_name_ms = {
    private_dns_name_ms = "selc-${local.env_short}-${local.domain}-iam-ms-ca.${local.private_dns_name_domain}"
  }

  naming_config = "documents"
}
