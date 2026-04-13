locals {
  # ============================================================
  # Constants — never change across environments
  # ============================================================
  prefix              = "selc"
  prefix_short        = "sc"
  location            = "westeurope"
  location_short      = "weu"
  location_pair       = "northeurope"
  location_pair_short = "neu"
  app_name            = "selfcare"
  app_domain          = "ar"
  external_domain     = "pagopa.it"

  # ============================================================
  # Bridge variables → locals
  # Keeps all local.X references in other files working unchanged
  # ============================================================
  env_short        = var.env_short
  env              = var.env
  aks_platform_env = var.aks_platform_env

  dns_zone_prefix         = var.dns_zone_prefix
  dns_zone_prefix_ar      = var.dns_zone_prefix_ar
  dns_ns_interop_selfcare = var.dns_ns_interop_selfcare

  app_gateway_api_certificate_name      = var.app_gateway_api_certificate_name
  app_gateway_api_pnpg_certificate_name = var.app_gateway_api_pnpg_certificate_name
  cdn_certificate_name_ar               = var.cdn_certificate_name_ar

  ca_suffix_dns_private_name      = var.ca_suffix_dns_private_name
  ca_pnpg_suffix_dns_private_name = var.ca_pnpg_suffix_dns_private_name
  auth_ms_private_dns_suffix      = var.auth_ms_private_dns_suffix

  redis_sku_name = var.redis_sku_name
  redis_capacity = var.redis_capacity

  aks_kubernetes_version                  = var.aks_kubernetes_version
  aks_sku_tier                            = var.aks_sku_tier
  aks_upgrade_settings_max_surge          = var.aks_upgrade_settings_max_surge
  aks_system_node_pool_vm_size            = var.aks_system_node_pool_vm_size
  aks_system_node_pool_os_disk_type       = var.aks_system_node_pool_os_disk_type
  aks_system_node_pool_node_count_min     = var.aks_system_node_pool_node_count_min
  aks_system_node_pool_node_count_max     = var.aks_system_node_pool_node_count_max
  system_node_pool_enable_host_encryption = var.system_node_pool_enable_host_encryption
  aks_user_node_pool_vm_size              = var.aks_user_node_pool_vm_size
  aks_user_node_pool_os_disk_type         = var.aks_user_node_pool_os_disk_type
  aks_user_node_pool_node_count_min       = var.aks_user_node_pool_node_count_min
  aks_user_node_pool_node_count_max       = var.aks_user_node_pool_node_count_max

  law_daily_quota_gb = var.law_daily_quota_gb

  cosmosdb_mongodb_extra_capabilities               = var.cosmosdb_mongodb_extra_capabilities
  cosmosdb_mongodb_main_geo_location_zone_redundant = var.cosmosdb_mongodb_main_geo_location_zone_redundant
  cosmosdb_mongodb_enable_autoscaling               = var.cosmosdb_mongodb_enable_autoscaling
  cosmosdb_mongodb_enable_free_tier                 = var.cosmosdb_mongodb_enable_free_tier
  cosmosdb_mongodb_additional_geo_locations         = var.cosmosdb_mongodb_additional_geo_locations

  cae_zone_redundant      = var.cae_zone_redundant
  cae_zone_redundant_pnpg = var.cae_zone_redundant_pnpg

  contracts_enable_versioning     = var.contracts_enable_versioning
  contracts_delete_retention_days = var.contracts_delete_retention_days

  enable_load_tests_db = var.enable_load_tests_db

  # ============================================================
  # Derived values
  # ============================================================
  project      = "${local.prefix}-${var.env_short}"
  project_pair = "${local.prefix}-${var.env_short}-${local.location_pair_short}"

  private_dns_name = "selc.internal.${var.dns_zone_prefix}.${local.external_domain}"

  tags = {
    CreatedBy   = "Terraform"
    Environment = title(var.env)
    Owner       = "Selfcare"
    Source      = "https://github.com/pagopa/selfcare"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  docker_registry = {
    sku                     = var.docker_registry_sku
    zone_redundancy_enabled = var.docker_registry_zone_redundancy_enabled
    geo_replication = {
      enabled                   = var.docker_registry_geo_replication_enabled
      regional_endpoint_enabled = var.docker_registry_geo_replication_enabled
      zone_redundancy_enabled   = var.docker_registry_geo_replication_enabled
    }
    network_rule_set = {
      default_action  = var.docker_registry_geo_replication_enabled ? "Allow" : "Deny"
      ip_rule         = []
      virtual_network = []
    }
  }

  # ============================================================
  # Networking — CIDR ranges (stable, no env differences in -ar)
  # ============================================================
  cidr_vnet                         = ["10.1.0.0/16"]
  cidr_subnet_k8s                   = ["10.1.0.0/17"]
  cidr_subnet_appgateway            = ["10.1.128.0/24"]
  cidr_subnet_cdn                   = ["10.1.129.0/24"] // condivisa con pnpg cdn
  cidr_subnet_azdoa                 = ["10.1.130.0/24"]
  cidr_subnet_redis                 = ["10.1.132.0/24"]
  cidr_subnet_vpn                   = ["10.1.133.0/24"]
  cidr_subnet_dns_forwarder         = ["10.1.134.0/29"]
  cidr_subnet_cosmosdb_mongodb      = ["10.1.135.0/24"]
  cidr_subnet_document_storage      = ["10.1.136.0/24"]
  cidr_subnet_contract_storage      = ["10.1.137.0/24"]
  cidr_subnet_eventhub              = ["10.1.138.0/24"]
  cidr_subnet_logs_storage          = ["10.1.139.0/24"]
  cidr_subnet_pnpg_cosmosdb_mongodb = ["10.1.140.0/24"]
  cidr_subnet_private_endpoints     = ["10.1.141.0/24"]
  cidr_subnet_load_tests            = ["10.1.142.0/24"]
  cidr_subnet_ai_search             = ["10.1.145.0/29"]
  cidr_subnet_eventhub_rds          = ["10.1.153.0/26"]
  cidr_subnet_selc                  = ["10.1.148.0/23"]
  cidr_subnet_selc_pnpg             = ["10.1.150.0/23"]

  cidr_pair_vnet                = ["10.101.0.0/16"]
  cidr_subnet_pair_dnsforwarder = ["10.101.134.0/29"]

  vnet_aks_ddos_protection_plan = false
  cidr_aks_platform_vnet        = ["10.11.0.0/16"]

  # ============================================================
  # Storage
  # ============================================================
  public_network_access_enabled = false

  # ============================================================
  # Azure DevOps
  # ============================================================
  azdo_sp_tls_cert_enabled     = true
  enable_azdoa                 = true
  enable_iac_pipeline          = true
  enable_app_projects_pipeline = true

  # ============================================================
  # Redis — fixed parameters
  # ============================================================
  redis_family                   = "C"
  redis_private_endpoint_enabled = true
  redis_version                  = 6

  # ============================================================
  # AKS — fixed parameters
  # ============================================================
  aks_alerts_enabled                                = false
  aks_system_node_pool_only_critical_addons_enabled = true
  aks_user_node_pool_enabled                        = true
  user_node_pool_node_labels = {
    "node_type" = "user"
  }
  reverse_proxy_ip = "10.1.1.250"

  # ============================================================
  # Monitoring — fixed parameters
  # ============================================================
  law_sku               = "PerGB2018"
  law_retention_in_days = 30

  # ============================================================
  # Spid / robots
  # ============================================================
  enable_spid_test      = false
  robots_indexed_paths  = []
  spid_selc_path_prefix = "/spid-login/v1"

  # ============================================================
  # VPN
  # ============================================================
  vpn_sku     = "VpnGw1"
  vpn_pip_sku = "Standard"

  private_endpoint_network_policies = "Enabled"

  # ============================================================
  # Contracts storage — fixed parameters
  # ============================================================
  contracts_advanced_threat_protection = false

  # ============================================================
  # EventHub — fixed parameters
  # ============================================================
  eventhub_sku_name                 = "Standard"
  eventhub_capacity                 = 2
  eventhub_auto_inflate_enabled     = true
  eventhub_maximum_throughput_units = 4
  eventhub_zone_redundant           = true
  eventhub_alerts_enabled           = false

  eventhub_ip_rules = [
    { ip_mask = "18.192.147.151", action = "Allow" }, // DATALAKE
    { ip_mask = "18.159.227.69", action = "Allow" },  // DATALAKE
    { ip_mask = "3.126.198.129", action = "Allow" },  // DATALAKE
    { ip_mask = "18.197.134.65", action = "Allow" },  // SAP
    { ip_mask = "52.29.190.137", action = "Allow" },  // SAP
    { ip_mask = "3.67.255.232", action = "Allow" },   // SAP
    { ip_mask = "3.67.182.154", action = "Allow" },   // SAP
    { ip_mask = "3.68.44.236", action = "Allow" },    // SAP
    { ip_mask = "3.66.249.150", action = "Allow" },   // SAP
    { ip_mask = "18.198.196.89", action = "Allow" },  // SAP
    { ip_mask = "18.193.21.232", action = "Allow" },  // SAP
    { ip_mask = "3.65.9.91", action = "Allow" },      // SAP
    { ip_mask = "91.218.226.5", action = "Allow" },   // FD
    { ip_mask = "91.218.226.15", action = "Allow" },  // FD
    { ip_mask = "91.218.224.5", action = "Allow" },   // FD
    { ip_mask = "91.218.224.15", action = "Allow" },  // FD
    { ip_mask = "2.228.86.218", action = "Allow" },   // FD
    { ip_mask = "185.170.36.80", action = "Allow" },  // KONECTA
    { ip_mask = "4.232.15.12", action = "Allow" },    // piattaforma-unitaria
    { ip_mask = "4.232.131.72", action = "Allow" },   // p4pa
  ]

  eventhub_rds_vm = {
    size = "Standard_B1ls"
    allowed_ipaddresses = [
      "*"
    ]
  }

  eventhubs = [{
    name              = "SC-Contracts"
    partitions        = 30
    message_retention = 7
    consumers         = ["conservazione", "interceptor", "datalake", "piattaforma-unitaria", "selc-proxy"]
    keys = [
      { name = "selfcare-wo", listen = false, send = true, manage = false },
      { name = "datalake", listen = true, send = false, manage = false },
      { name = "pn", listen = true, send = false, manage = false },
      { name = "interceptor", listen = true, send = false, manage = false },
      { name = "io-sign", listen = true, send = false, manage = false },
      { name = "external-interceptor", listen = true, send = false, manage = false },
      { name = "interop", listen = true, send = false, manage = false },
      { name = "sma", listen = true, send = false, manage = false },
      { name = "conservazione", listen = true, send = false, manage = false },
      { name = "piattaforma-unitaria", listen = true, send = false, manage = false },
      { name = "selc-proxy", listen = true, send = false, manage = false },
    ]
    }, {
    name              = "Selfcare-FD"
    partitions        = 5
    message_retention = 7
    consumers         = []
    keys = [
      { name = "external-interceptor-wo", listen = false, send = true, manage = false },
      { name = "fd", listen = true, send = false, manage = false },
    ]
    }, {
    name              = "SC-Contracts-sap"
    partitions        = 5
    message_retention = 7
    consumers         = []
    keys = [
      { name = "sap", listen = true, send = false, manage = false },
      { name = "external-interceptor-wo", listen = false, send = true, manage = false },
    ]
    }, {
    name              = "SC-Users"
    partitions        = 10
    message_retention = 7
    consumers         = ["datalake", "interop", "piattaforma-unitaria"]
    keys = [
      { name = "selfcare-wo", listen = false, send = true, manage = false },
      { name = "datalake", listen = true, send = false, manage = false },
      { name = "external-interceptor", listen = true, send = false, manage = false },
      { name = "sma", listen = true, send = false, manage = false },
      { name = "interop", listen = true, send = false, manage = false },
      { name = "piattaforma-unitaria", listen = true, send = false, manage = false },
    ]
    }, {
    name              = "SC-UserGroups"
    partitions        = 10
    message_retention = 7
    consumers         = ["io-cms-sync", "piattaforma-unitaria"]
    keys = [
      { name = "selfcare-wo", listen = false, send = true, manage = false },
      { name = "io", listen = true, send = false, manage = false },
      { name = "piattaforma-unitaria", listen = true, send = false, manage = false },
    ]
    }, {
    name              = "SC-Delegations"
    partitions        = 10
    message_retention = 7
    consumers         = ["datalake"]
    keys = [
      { name = "selfcare-wo", listen = false, send = true, manage = false },
      { name = "datalake", listen = true, send = false, manage = false },
    ]
  }]

  # ============================================================
  # Lock
  # ============================================================
  lock_enable = false
}
