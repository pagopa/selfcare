# ==============================================================================
# Environment identity
# ==============================================================================

env       = "dev"
env_short = "d"

aks_platform_env = "dev01"

# ==============================================================================
# DNS
# ==============================================================================

dns_zone_prefix    = "dev.selfcare"
dns_zone_prefix_ar = "dev.areariservata"

dns_ns_interop_selfcare = [
  "ns-774.awsdns-32.net",
  "ns-394.awsdns-49.com",
  "ns-1704.awsdns-21.co.uk",
  "ns-1091.awsdns-08.org",
]

# ==============================================================================
# Certificates
# ==============================================================================

app_gateway_api_certificate_name      = "api-dev-selfcare-pagopa-it"
app_gateway_api_pnpg_certificate_name = "api-pnpg-dev-selfcare-pagopa-it"
cdn_certificate_name_ar               = "dev-areariservata-pagopa-it"

# ==============================================================================
# Container App Environment DNS suffixes
# NOTE: update these whenever the CAE is recreated
# ==============================================================================

ca_suffix_dns_private_name      = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
ca_pnpg_suffix_dns_private_name = "blackhill-644148c0.westeurope.azurecontainerapps.io"
auth_ms_private_dns_suffix      = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"

# ==============================================================================
# Redis — Basic SKU for dev
# ==============================================================================

redis_sku_name = "Basic"
redis_capacity = 0

# ==============================================================================
# AKS — minimal sizing for dev
# ==============================================================================

aks_system_node_pool_vm_size        = "Standard_B4ms"
aks_system_node_pool_os_disk_type   = "Managed"
aks_system_node_pool_node_count_min = 1
aks_system_node_pool_node_count_max = 1

aks_user_node_pool_vm_size        = "Standard_B4ms"
aks_user_node_pool_os_disk_type   = "Managed"
aks_user_node_pool_node_count_min = 1
aks_user_node_pool_node_count_max = 3

# ==============================================================================
# Docker Registry — Basic, no geo-replication for dev
# ==============================================================================

docker_registry_sku                     = "Basic"
docker_registry_zone_redundancy_enabled = false
docker_registry_geo_replication_enabled = false

# ==============================================================================
# Monitoring — 2 GB daily quota for dev
# ==============================================================================

law_daily_quota_gb = 2

# ==============================================================================
# CosmosDB — serverless for dev
# ==============================================================================

cosmosdb_mongodb_extra_capabilities               = ["EnableServerless", "EnableMongoRoleBasedAccessControl"]
cosmosdb_mongodb_main_geo_location_zone_redundant = false
cosmosdb_mongodb_enable_autoscaling               = false
cosmosdb_mongodb_enable_free_tier                 = false
cosmosdb_mongodb_additional_geo_locations         = []

# ==============================================================================
# Container App Environments — no zone redundancy for dev
# ==============================================================================

cae_zone_redundant      = false
cae_zone_redundant_pnpg = false

# ==============================================================================
# Contracts storage — no versioning for dev
# ==============================================================================

contracts_enable_versioning     = false
contracts_delete_retention_days = 0

# ==============================================================================
# Feature flags
# ==============================================================================

enable_load_tests_db = true
