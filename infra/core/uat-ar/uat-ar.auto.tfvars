# ==============================================================================
# Environment identity
# ==============================================================================

env       = "uat"
env_short = "u"

aks_platform_env = "uat01"

# ==============================================================================
# DNS
# ==============================================================================

dns_zone_prefix    = "uat.selfcare"
dns_zone_prefix_ar = "uat.areariservata"

dns_ns_interop_selfcare = [
  "ns-875.awsdns-45.net",
  "ns-1323.awsdns-37.org",
  "ns-387.awsdns-48.com",
  "ns-2032.awsdns-62.co.uk",
]

# ==============================================================================
# Certificates
# ==============================================================================

app_gateway_api_certificate_name      = "api-uat-selfcare-pagopa-it"
app_gateway_api_pnpg_certificate_name = "api-pnpg-uat-selfcare-pagopa-it"
cdn_certificate_name_ar               = "uat-areariservata-pagopa-it"

# ==============================================================================
# Container App Environment DNS suffixes
# NOTE: update these whenever the CAE is recreated
# ==============================================================================

ca_suffix_dns_private_name      = "mangopond-2a5d4d65.westeurope.azurecontainerapps.io"
ca_pnpg_suffix_dns_private_name = "orangeground-0bd2d4dc.westeurope.azurecontainerapps.io"
auth_ms_private_dns_suffix      = "mangopond-2a5d4d65.westeurope.azurecontainerapps.io"

# ==============================================================================
# Redis — Standard SKU for uat
# ==============================================================================

redis_sku_name = "Standard"
redis_capacity = 0

# ==============================================================================
# AKS — minimal sizing for uat
# ==============================================================================

aks_system_node_pool_vm_size        = "Standard_B4ms"
aks_system_node_pool_os_disk_type   = "Managed"
aks_system_node_pool_node_count_min = 1
aks_system_node_pool_node_count_max = 1

aks_user_node_pool_vm_size        = "Standard_B4ms"
aks_user_node_pool_os_disk_type   = "Managed"
aks_user_node_pool_node_count_min = 2
aks_user_node_pool_node_count_max = 4

# ==============================================================================
# Docker Registry — Basic, no geo-replication for uat
# ==============================================================================

docker_registry_sku                     = "Basic"
docker_registry_zone_redundancy_enabled = false
docker_registry_geo_replication_enabled = false

# ==============================================================================
# Monitoring — 2 GB daily quota for uat
# ==============================================================================

law_daily_quota_gb = 2

# ==============================================================================
# CosmosDB — serverless for uat
# ==============================================================================

cosmosdb_mongodb_extra_capabilities               = ["EnableServerless", "EnableMongoRoleBasedAccessControl"]
cosmosdb_mongodb_main_geo_location_zone_redundant = false
cosmosdb_mongodb_enable_autoscaling               = false
cosmosdb_mongodb_enable_free_tier                 = false
cosmosdb_mongodb_additional_geo_locations         = []

# ==============================================================================
# Container App Environments — no zone redundancy for uat
# ==============================================================================

cae_zone_redundant      = false
cae_zone_redundant_pnpg = false

# ==============================================================================
# Contracts storage
# ==============================================================================

contracts_enable_versioning     = false
contracts_delete_retention_days = 10

# ==============================================================================
# Feature flags
# ==============================================================================

enable_load_tests_db = true
