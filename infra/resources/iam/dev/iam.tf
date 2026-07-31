###############################################################################
# UNIFIED (MULTITENANT) STACK — dev
#
# One deployment of iam-ms serving BOTH tenants (AR and PNPG), replacing the
# separate `dev-ar` + `dev-pnpg` stacks. See EPIC.md sub-task 7 and README.md in
# this folder for the cutover procedure and its prerequisites.
#
# What actually changes versus the two legacy stacks:
#   - ONE container app instead of two (the AR one survives; the PNPG one is
#     decommissioned).
#   - ONE Cosmos database instead of two. Tenants are separated inside it by the
#     `tenantId` discriminator (sub-task 6), not by account.
#   - TWO APIM APIs are KEPT, one per tenant-facing URL, both pointing at the
#     single backend. The PNPG frontend keeps calling
#     api-pnpg.dev.selfcare.pagopa.it/imprese/iam and needs no change; only what
#     sits behind APIM is consolidated. Collapsing the two URLs into one is a
#     separate, frontend-visible decision and is deliberately NOT done here.
###############################################################################

module "local" {
  source = "../../_modules/local-env"

  env       = "dev"
  env_short = "d"

  # The surviving infrastructure is the AR one: its container app environment,
  # Cosmos account and Key Vault are the consolidation target. `domain` drives
  # those names in _modules/local-env, so it stays "ar" even though the stack now
  # serves both tenants. It is NOT a statement about which tenants are served.
  domain = "ar"

  dns_zone_prefix                = "dev.selfcare"
  api_dns_zone_prefix            = "api.dev.selfcare"
  private_dns_name_domain        = "whitemoss-eb7ef327.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-cae-002"
  ca_resource_group_name         = "selc-d-container-app-002-rg"
  container_app_min_replicas     = 0
}

locals {
  # The single backend both APIM APIs forward to. Defined once precisely so that
  # the two APIs cannot drift apart and quietly start serving different
  # deployments again.
  iam_ms_private_dns_name = "selc-${module.local.config.env_short}-iam-ms-ca.${module.local.config.private_dns_name_domain}"

  # PNPG frontend-facing DNS. Not taken from module.local.config, which resolves
  # to the AR zone because this stack's `domain` is "ar".
  pnpg_dns_zone_prefix     = "pnpg.dev.selfcare"
  pnpg_api_dns_zone_prefix = "api-pnpg.dev.selfcare"
}

###############################################################################
# APIM — one API per tenant-facing URL, both backed by the same container app
#
# Each API resolves X-Tenant-Id from the calling origin against
# module.local.config.tenant_ids (which lists AR and PNPG) and always discards any
# X-Tenant-Id supplied by the caller. Tenant identity therefore comes from the
# URL the browser actually used, not from which API definition was hit, so the
# two APIs cannot be played off against each other by calling the "wrong" one.
###############################################################################

module "apim_api_ar" {
  source              = "../../_modules/apim_api"
  apim_name           = module.local.config.apim_name
  apim_rg             = module.local.config.apim_rg
  api_name            = "selc-${module.local.config.env_short}-api-iam"
  display_name        = "IAM API"
  base_path           = "iam"
  private_dns_name    = local.iam_ms_private_dns_name
  dns_zone_prefix     = module.local.config.dns_zone_prefix
  api_dns_zone_prefix = module.local.config.api_dns_zone_prefix
  openapi_path        = "../../../../apps/iam/src/main/docs/openapi.json"

  api_operation_policies    = []
  tenant_ids                = module.local.config.tenant_ids
  local_development_origins = module.local.config.local_development_origins
}

module "apim_api_pnpg" {
  source              = "../../_modules/apim_api"
  apim_name           = module.local.config.apim_name
  apim_rg             = module.local.config.apim_rg
  api_name            = "selc-${module.local.config.env_short}-api-iam-pnpg"
  display_name        = "IAM API PNPG"
  base_path           = "imprese/iam"
  private_dns_name    = local.iam_ms_private_dns_name
  dns_zone_prefix     = local.pnpg_dns_zone_prefix
  api_dns_zone_prefix = local.pnpg_api_dns_zone_prefix
  openapi_path        = "../../../../apps/iam/src/main/docs/openapi.json"

  api_operation_policies    = []
  tenant_ids                = module.local.config.tenant_ids
  local_development_origins = module.local.config.local_development_origins
}

###############################################################################
# CosmosDB — single database for both tenants
#
# PREREQUISITE: the PNPG userClaims/roles documents must already have been copied
# into this database with `tenantId: "PNPG"` set. See README.md. Applying this
# stack does not move any data.
###############################################################################

module "cosmosdb" {
  source = "../../_modules/cosmosdb_database"

  database_name               = "selcIam"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
}

module "collection_iam_user" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "userClaims"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  # `email` is unique per tenant, not globally: the same person can hold claims
  # under both tenants. The unique index therefore has to become composite with
  # tenantId, otherwise merging the PNPG documents into this collection fails on
  # duplicate keys for every user present in both. This index change MUST be
  # applied before the data migration, not after.
  indexes = [
    { keys = ["_id"], unique = true },
    { keys = ["tenantId", "email"], unique = true },
    { keys = ["tenantId"], unique = false }
  ]

  depends_on = [module.cosmosdb]
}

module "collection_iam_roles" {
  source = "../../_modules/cosmosdb_collection"

  name                        = "roles"
  resource_group_name         = module.local.config.mongo_db.mongodb_rg_name
  cosmosdb_mongo_account_name = module.local.config.mongo_db.cosmosdb_account_mongodb_name
  database_name               = module.cosmosdb.database_name

  lock_enable = true

  # No tenant index: `roles` is the global role catalogue, shared by every tenant
  # and deliberately not tenant-scoped (see EPIC.md sub-task 6, iam section).
  indexes = [
    { keys = ["_id"], unique = true }
  ]

  depends_on = [module.cosmosdb]
}

###############################################################################
# Container App — the single deployment serving both tenants
###############################################################################

locals {
  app_settings_iam_ms = [
    {
      name = "JAVA_TOOL_OPTIONS"
      # The DNS-caching and IPv4 flags come from the legacy PNPG stack, which had
      # them while AR did not. Kept, because the consolidated app now carries the
      # PNPG traffic that motivated them; dropping them would silently change
      # behaviour for those users.
      value = "-javaagent:applicationinsights-agent.jar -Djava.net.preferIPv4Stack=true -Dnetworkaddress.cache.ttl=30 -Dnetworkaddress.cache.negative.ttl=1"
    },
    {
      name  = "APPLICATIONINSIGHTS_ROLE_NAME"
      value = "iam-ms"
    },
    {
      name  = "SHARED_ACCESS_KEY_NAME"
      value = "selfcare-wo"
    },
    {
      name  = "IAM_MS_RETRY_MIN_BACKOFF"
      value = 5
    },
    {
      name  = "IAM_MS_RETRY_MAX_BACKOFF"
      value = 60
    },
    {
      name  = "IAM_MS_RETRY"
      value = 3
    },
    {
      name  = "INSTITUTION_API_URL"
      value = "https://selc-${module.local.config.env_short}-institution-ms-ca.${module.local.config.private_dns_name_domain}"
    }
  ]

  # Both tenants' requests are served with the same secrets. This is only correct
  # because these are platform secrets (telemetry, JWT verification key, database
  # connection) and not tenant-owned material. Any secret that genuinely differs
  # per tenant must NOT be added here as a single value — it needs a per-tenant
  # env var (e.g. FOO_AR / FOO_PNPG) resolved by the app from the tenant, which is
  # the pattern sub-task 7 mandates.
  #
  # PREREQUISITE: JWT_PUBLIC_KEY must verify tokens from BOTH issuers. PNPG
  # sessions are signed by hub-spid-login, AR sessions by `auth`; the legacy
  # stacks each held only their own key. If the two issuers use different keys,
  # this single value is wrong and multi-issuer verification has to land first.
  # See README.md.
  secrets_names_iam_ms = {
    "APPLICATIONINSIGHTS_CONNECTION_STRING" = "appinsights-connection-string"
    "SELFCARE_DATA_ENCRIPTION_KEY"          = "selfcare-data-encryption-key"
    "SELFCARE_DATA_ENCRIPTION_IV"           = "selfcare-data-encryption-iv"
    "MONGODB_CONNECTION_STRING"             = "mongodb-connection-string"
    "JWT_PUBLIC_KEY"                        = "jwt-public-key"
  }
}

module "container_app_iam_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-iam-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-iam-ms"
  image_tag                      = var.image_tag
  app_settings                   = local.app_settings_iam_ms
  secrets_names                  = local.secrets_names_iam_ms
  key_vault_resource_group_name  = module.local.config.key_vault_resource_group_name
  key_vault_name                 = module.local.config.key_vault_name
  probes                         = module.local.config.quarkus_health_probes
  tags                           = module.local.config.tags
}
