locals {
  # general
  prefix              = "selc"
  env_short           = "u"
  env                 = "uat"
  location            = "westeurope"
  location_short      = "weu"
  location_pair       = "northeurope"
  location_pair_short = "neu"
  app_name            = "selfcare"
  app_domain          = "pnpg"

  project      = "${local.prefix}-${local.env_short}"
  project_pair = "${local.prefix}-${local.env_short}-${local.location_pair_short}"

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Uat"
    Owner       = "Selfcare"
    Source      = "https://github.com/pagopa/selfcare"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
    Application = "PNPG"
  }

  lock_enable = false

  # networking
  # main vnet
  cidr_vnet                         = ["10.1.0.0/16"]
  cidr_subnet_k8s                   = ["10.1.0.0/17"]
  cidr_subnet_appgateway            = ["10.1.128.0/24"]
  cidr_subnet_cdn                   = ["10.1.129.0/24"] // condivisa con ar cdn
  cidr_subnet_azdoa                 = ["10.1.130.0/24"]
  cidr_subnet_redis                 = ["10.1.132.0/24"]
  cidr_subnet_vpn                   = ["10.1.133.0/24"]
  cidr_subnet_dns_forwarder         = ["10.1.134.0/29"]
  cidr_subnet_cosmosdb_mongodb      = ["10.1.135.0/24"]
  cidr_subnet_document_storage      = ["10.1.136.0/24"] #this is a place holder for document storage account
  cidr_subnet_contract_storage      = ["10.1.137.0/24"]
  cidr_subnet_eventhub              = ["10.1.138.0/24"]
  cidr_subnet_logs_storage          = ["10.1.139.0/24"]
  cidr_subnet_private_endpoints     = ["10.1.140.0/24"] #this is a place holder for pnpg mongo
  cidr_subnet_pnpg_cosmosdb_mongodb = ["10.1.141.0/24"]
  cidr_subnet_load_tests            = ["10.1.142.0/24"]
  cidr_subnet_pnpg_redis            = ["10.1.143.0/29"]
  cidr_subnet_pnpg_logs_storage     = ["10.1.143.8/29"]
  cidr_subnet_ai_search             = ["10.1.145.0/29"]
  cidr_subnet_eventhub_rds          = ["10.1.153.0/26"]


  cidr_subnet_selc      = ["10.1.148.0/23"]
  cidr_subnet_selc_pnpg = ["10.1.150.0/23"]

  #
  # Pair VNET
  #
  cidr_pair_vnet                = ["10.101.0.0/16"]
  cidr_subnet_pair_dnsforwarder = ["10.101.134.0/29"]

  #
  # AKS Platform
  #
  aks_platform_env              = "uat01"
  vnet_aks_ddos_protection_plan = false
  cidr_aks_platform_vnet        = ["10.11.0.0/16"]
  ingress_load_balancer_ip      = "10.11.100.250"

  # dns
  dns_zone_prefix    = "uat.selfcare"
  dns_zone_prefix_ar = "uat.areariservata"
  external_domain    = "pagopa.it"

  # storage account
  public_network_access_enabled = false

  # azure devops
  azdo_sp_tls_cert_enabled     = true
  enable_azdoa                 = true
  enable_iac_pipeline          = true
  enable_app_projects_pipeline = true

  # app_gateway
  app_gateway_api_certificate_name      = "api-dev-selfcare-pagopa-it"
  app_gateway_api_pnpg_certificate_name = "api-pnpg-dev-selfcare-pagopa-it"

  # redis
  redis_sku_name                 = "Basic"
  redis_family                   = "C"
  redis_capacity                 = 0
  redis_private_endpoint_enabled = true
  redis_version                  = 6

  # aks
  aks_alerts_enabled                  = false
  aks_kubernetes_version              = "1.27.7"
  aks_system_node_pool_os_disk_type   = "Managed"
  aks_system_node_pool_node_count_min = 1
  aks_system_node_pool_node_count_max = 1

  # This is the k8s ingress controller ip. It must be in the aks subnet range.
  reverse_proxy_ip                = "10.1.1.250"
  private_dns_name                = "selc.internal.dev.selfcare.pagopa.it"
  ca_suffix_dns_private_name      = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  ca_pnpg_suffix_dns_private_name = "blackhill-644148c0.westeurope.azurecontainerapps.io"
  spid_selc_path_prefix           = "/spid-login/v1"


  aks_system_node_pool_vm_size                      = "Standard_B4ms"
  aks_system_node_pool_only_critical_addons_enabled = true

  aks_user_node_pool_enabled        = true
  aks_user_node_pool_os_disk_type   = "Managed"
  aks_user_node_pool_node_count_min = 1
  aks_user_node_pool_node_count_max = 3
  user_node_pool_node_labels = {
    "node_type" = "user"
  }

  #
  # Docker
  #
  docker_registry = {
    sku                     = "Basic"
    zone_redundancy_enabled = false
    geo_replication = {
      enabled                   = false
      regional_endpoint_enabled = false
      zone_redundancy_enabled   = false
    }
    network_rule_set = {
      default_action  = "Deny"
      ip_rule         = []
      virtual_network = []
    }
  }

  # monitoring
  law_sku               = "PerGB2018"
  law_retention_in_days = 30
  law_daily_quota_gb    = 2

  # CosmosDb MongoDb
  cosmosdb_mongodb_offer_type                    = "Standard"
  cosmosdb_mongodb_public_network_access_enabled = false
  cosmosdb_mongodb_consistency_policy = {
    consistency_level       = "Session"
    max_interval_in_seconds = null
    max_staleness_prefix    = null
  }
  cosmosdb_mongodb_extra_capabilities               = []
  cosmosdb_mongodb_main_geo_location_zone_redundant = true
  cosmosdb_mongodb_additional_geo_locations         = []
  cosmosdb_mongodb_throughput                       = 1000
  cosmosdb_mongodb_max_throughput                   = 1000
  cosmosdb_mongodb_enable_autoscaling               = true
  cosmosdb_mongodb_private_endpoint_enabled         = true

  # spid-testenv
  enable_spid_test = false

  robots_indexed_paths = []

  enable_load_tests_db = true

  cae_zone_redundant      = false
  cae_zone_redundant_pnpg = false

  auth_ms_private_dns_suffix = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"

  vpn_sku     = "VpnGw1"
  vpn_pip_sku = "Standard"

  private_endpoint_network_policies = "Enabled"

  contracts_enable_versioning          = false
  contracts_advanced_threat_protection = false
  contracts_delete_retention_days      = 0

  # Storage Logs
  # logs storage
  logs_account_replication_type   = "LRS"
  logs_delete_retention_days      = 14
  logs_enable_versioning          = false
  logs_advanced_threat_protection = true

  monitor_action_group_slack_name    = "SlackPagoPA"
  monitor_action_group_email_name    = "PagoPA"
  monitor_action_group_opsgenie_name = "Opsgenie"
  alert_action_group_domain_name     = "${local.prefix}${local.env_short}${local.app_domain}"

  # Monitor
  app_name_fn                           = "${local.prefix}-${local.env_short}-pnpg-onboarding-fn"
  alert_functions_exceptions_name       = "pnpg-functions-exception"
  alert_functions_exceptions_role_names = ["${local.app_name_fn}"]

  api_gateway_url  = "https://api-pnpg.uat.selfcare.pagopa.it"
  spid_testenv_url = "https://selc-u-pnpg-spid-testenv.westeurope.azurecontainer.io"

  # jwt exchange duration
  jwt_token_exchange_duration = "PT15M"

  # session jwt audience
  jwt_audience      = "api-pnpg.uat.selfcare.pagopa.it"
  jwt_issuer        = "SPID"
  jwt_social_expire = "10000000"
}