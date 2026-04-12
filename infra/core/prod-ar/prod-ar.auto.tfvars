# ==============================================================================
# Environment identity
# ==============================================================================

env       = "prod"
env_short = "p"

aks_platform_env = "prod01"

# ==============================================================================
# DNS
# ==============================================================================

dns_zone_prefix    = "selfcare"
dns_zone_prefix_ar = "areariservata"

dns_ns_interop_selfcare = [
  "ns-1355.awsdns-41.org",
  "ns-601.awsdns-11.net",
  "ns-2030.awsdns-61.co.uk",
  "ns-119.awsdns-14.com",
]

# ==============================================================================
# Certificates
# ==============================================================================

app_gateway_api_certificate_name      = "api-selfcare-pagopa-it"
app_gateway_api_pnpg_certificate_name = "api-pnpg-selfcare-pagopa-it"
# cdn_certificate_name_ar not needed for prod (uses default "")

# ==============================================================================
# Container App Environment DNS suffixes
# NOTE: update these whenever the CAE is recreated
# ==============================================================================

ca_suffix_dns_private_name      = "lemonpond-bb0b750e.westeurope.azurecontainerapps.io"
ca_pnpg_suffix_dns_private_name = "calmmoss-0be48755.westeurope.azurecontainerapps.io"
auth_ms_private_dns_suffix      = "lemonpond-bb0b750e.westeurope.azurecontainerapps.io"

# ==============================================================================
# Redis — Standard SKU for prod
# ==============================================================================

redis_sku_name = "Standard"
redis_capacity = 0

# ==============================================================================
# AKS — production sizing
# ==============================================================================

aks_sku_tier                   = "Standard"
aks_upgrade_settings_max_surge = "33%"

aks_system_node_pool_vm_size            = "Standard_D4ds_v5"
aks_system_node_pool_os_disk_type       = "Ephemeral"
aks_system_node_pool_node_count_min     = 2
aks_system_node_pool_node_count_max     = 3
system_node_pool_enable_host_encryption = true

aks_user_node_pool_vm_size        = "Standard_D4ds_v5"
aks_user_node_pool_os_disk_type   = "Ephemeral"
aks_user_node_pool_node_count_min = 3
aks_user_node_pool_node_count_max = 5

# ==============================================================================
# Docker Registry — Premium with geo-replication for prod
# ==============================================================================

docker_registry_sku                     = "Premium"
docker_registry_zone_redundancy_enabled = true
docker_registry_geo_replication_enabled = true

# ==============================================================================
# Monitoring — unlimited quota for prod
# ==============================================================================

law_daily_quota_gb = -1

# ==============================================================================
# CosmosDB — autoscaling, geo-redundant for prod
# ==============================================================================

cosmosdb_mongodb_extra_capabilities               = []
cosmosdb_mongodb_main_geo_location_zone_redundant = true
cosmosdb_mongodb_enable_autoscaling               = true
cosmosdb_mongodb_enable_free_tier                 = true
cosmosdb_mongodb_additional_geo_locations = [{
  location          = "northeurope"
  failover_priority = 1
  zone_redundant    = false
}]

# ==============================================================================
# Container App Environments — zone redundant for prod
# ==============================================================================

cae_zone_redundant      = true
cae_zone_redundant_pnpg = true

# ==============================================================================
# Contracts storage — versioning enabled for prod
# ==============================================================================

contracts_enable_versioning     = true
contracts_delete_retention_days = 14

# ==============================================================================
# Feature flags
# ==============================================================================

enable_load_tests_db = false
