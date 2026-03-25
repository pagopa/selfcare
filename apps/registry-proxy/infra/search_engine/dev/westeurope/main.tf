terraform {

  backend "azurerm" {
    resource_group_name  = "terraform-state-rg"
    storage_account_name = "tfappdevselfcare"
    container_name       = "terraform-state"
    key                  = "selfcare-party-registry-proxy.search_engine.tfstate"
  }
}

provider "azurerm" {
  features {}
}


module "ai_search" {
  source      = "../../_modules/ai_search"
  app_name    = "${local.prefix}-srch"
  prefix      = local.prefix
  env_short   = local.env_short
  project     = local.project
  location    = local.location
  domain      = "ar"
  sku         = "basic"
  tags        = local.tags
  cidr_subnet = ["10.1.145.0/29"]

  key_vault_name                = "selc-${local.env_short}-kv"
  key_vault_resource_group_name = "selc-${local.env_short}-sec-rg"
  public_network_access_enabled = false
  srch_private_endpoint_enabled = true
}

module "dapr" {
  source = "../../_modules/dapr"

  project   = local.project
  env_short = local.env_short

  cae_name    = "selc-${local.env_short}-cae-002"
  cae_rg_name = "selc-${local.env_short}-container-app-002-rg"
  ca_name     = "selc-${local.env_short}-party-reg-proxy-ca"
  ca_rg_name  = "selc-${local.env_short}-container-app-002-rg"

  key_vault_name                   = "selc-${local.env_short}-kv"
  key_vault_resource_group_name    = "selc-${local.env_short}-sec-rg"
  key_vault_event_hub_consumer_key = "eventhub-sc-contracts-selc-proxy-key-lc"

  queue_url            = "selc-${local.env_short}-eventhub-ns.servicebus.windows.net"
  queue_port           = "9093"
  queue_consumer_group = "party-proxy"
  queue_topic          = "SC-Contracts"

  #redis
  redis_enable                   = true
  redis_private_endpoint_enabled = true
  redis_capacity                 = 0
  redis_version                  = 6
  redis_family                   = "C"
  redis_sku_name                 = "Basic"

  search_service_name = module.ai_search.search_service_name
  search_service_key  = module.ai_search.search_service_admin_key

  tags = local.tags

}