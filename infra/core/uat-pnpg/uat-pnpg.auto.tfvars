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

# ==============================================================================
# Certificates
# ==============================================================================

app_gateway_api_certificate_name      = "api-dev-selfcare-pagopa-it"
app_gateway_api_pnpg_certificate_name = "api-pnpg-dev-selfcare-pagopa-it"

# ==============================================================================
# Container App Environment DNS suffixes
# NOTE: update these whenever the CAE is recreated
# ==============================================================================

ca_suffix_dns_private_name      = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
ca_pnpg_suffix_dns_private_name = "blackhill-644148c0.westeurope.azurecontainerapps.io"
auth_ms_private_dns_suffix      = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"

# ==============================================================================
# Redis
# ==============================================================================

redis_sku_name                 = "Basic"
redis_capacity                 = 0
redis_private_endpoint_enabled = true

# ==============================================================================
# AKS
# ==============================================================================

aks_system_node_pool_vm_size        = "Standard_B4ms"
aks_system_node_pool_node_count_min = 1
aks_system_node_pool_node_count_max = 1
aks_user_node_pool_node_count_min   = 1
aks_user_node_pool_node_count_max   = 3

# ==============================================================================
# Docker Registry
# ==============================================================================

docker_registry_sku                     = "Basic"
docker_registry_zone_redundancy_enabled = false
docker_registry_geo_replication_enabled = false

# ==============================================================================
# Monitoring
# ==============================================================================

law_daily_quota_gb = 2

# ==============================================================================
# CosmosDB
# ==============================================================================

cosmosdb_mongodb_extra_capabilities               = []
cosmosdb_mongodb_main_geo_location_zone_redundant = true
cosmosdb_mongodb_additional_geo_locations         = []
cosmosdb_mongodb_enable_autoscaling               = true
cosmosdb_mongodb_throughput                       = 1000
cosmosdb_mongodb_max_throughput                   = 1000
cosmosdb_mongodb_private_endpoint_enabled         = true

# ==============================================================================
# Container App Environments
# ==============================================================================

cae_zone_redundant      = false
cae_zone_redundant_pnpg = false

# ==============================================================================
# Contracts storage
# ==============================================================================

contracts_enable_versioning     = false
contracts_delete_retention_days = 0

# ==============================================================================
# Feature flags
# ==============================================================================

enable_load_tests_db = true

# ==============================================================================
# PNPG — API & JWT
# ==============================================================================

api_gateway_url             = "https://api-pnpg.uat.selfcare.pagopa.it"
spid_testenv_url            = "https://selc-u-pnpg-spid-testenv.westeurope.azurecontainer.io"
jwt_token_exchange_duration = "PT15M"
jwt_audience                = "api-pnpg.uat.selfcare.pagopa.it"
jwt_issuer                  = "SPID"
jwt_social_expire           = "10000000"
