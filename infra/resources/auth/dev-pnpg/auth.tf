module "local" {
  source = "../../_modules/local-env"

  env                            = "dev"
  env_short                      = "d"
  domain                         = "pnpg"
  dns_zone_prefix                = "pnpg.dev.selfcare"
  api_dns_zone_prefix            = "api-pnpg.dev.selfcare"
  private_dns_name_domain        = "blackhill-644148c0.westeurope.azurecontainerapps.io"
  container_app_environment_name = "selc-d-pnpg-cae-cp"
  ca_resource_group_name         = "selc-d-container-app-rg"
  container_app_min_replicas     = 1
}

locals {
  tenant = upper(module.local.config.domain)
  port   = 8082
  app_settings = {
    APPLICATIONINSIGHTS_ROLE_NAME = "pnpg-auth-ms"
    JAVA_TOOL_OPTIONS = join(" ", [
      "-javaagent:applicationinsights-agent.jar",
      "-Dio.smallrye.health.check.it.pagopa.selfcare.auth.health.AuthMongoReadinessCheck.enabled=false",
      "-Dquarkus.http.auth.permission.probes.paths=/q/health/*",
      "-Dquarkus.http.auth.permission.probes.policy=permit",
      "-Dquarkus.http.auth.permission.other.paths=/*",
      "-Dquarkus.http.auth.permission.other.policy=deny"
    ])
    M2M_ISSUER_ENABLED                     = "true"
    M2M_INTERNAL_PORT                      = tostring(local.port)
    M2M_TLS_ENABLED                        = "false"
    M2M_ALLOWED_TENANTS                    = local.tenant
    TENANT_SUPPORTED_TENANTS               = local.tenant
    TENANT_MONGO_MANDATORY                 = "false"
    TENANT_ONE_IDENTITY_MANDATORY_TENANTS  = ""
    TENANT_USER_REGISTRY_MANDATORY_TENANTS = ""
    SESSION_TOKEN_TENANT_AUDIENCE_ENABLED  = "true"
    # CDI validates the unused login clients at startup. No real API credentials are required.
    INTERNAL_API_KEY         = "not-used"
    INTERNAL_MS_USER_API_KEY = "not-used"
    INTERNAL_API_URL         = "http://127.0.0.1:1"
    INTERNAL_MS_USER_API_URL = "http://127.0.0.1:1"
    TENANT_REGISTRY_JSON = jsonencode({
      (local.tenant) = {
        auth_enabled = false
        jwt = {
          session = {
            privateKeyEnvVar = "TENANT_PNPG_JWT_SESSION_PRIVATE_KEY"
            keyIdEnvVar      = "TENANT_PNPG_JWT_SESSION_KEY_ID"
            audience         = module.local.config.tenant_registry[local.tenant].jwt_audience
          }
        }
        m2m = {
          enabled   = true
          uidEnvVar = "TENANT_PNPG_M2M_IAM_UID"
        }
      }
    })
  }
}

module "container_app_auth_ms" {
  source = "../../_modules/container_app_microservice"

  env_short                      = module.local.config.env_short
  resource_group_name            = module.local.config.ca_resource_group_name
  container_app                  = module.local.config.container_app
  container_app_name             = "${module.local.config.project}-${module.local.config.domain}-auth-ms"
  container_app_environment_name = module.local.config.container_app_environment_name
  image_name                     = "selfcare-auth-ms"
  image_tag                      = var.image_tag
  internal_tcp_ports             = [local.port]
  app_settings                   = [for name, value in local.app_settings : { name = name, value = value }]
  secrets_names = {
    APPLICATIONINSIGHTS_CONNECTION_STRING = "appinsights-connection-string"
    TENANT_PNPG_JWT_SESSION_PRIVATE_KEY   = "jwt-private-key-pkcs8"
    TENANT_PNPG_JWT_SESSION_KEY_ID        = "jwt-kid"
    TENANT_PNPG_M2M_IAM_UID               = "auth-m2m-iam-uid-pnpg"
  }
  key_vault_resource_group_name = module.local.config.key_vault_resource_group_name
  key_vault_name                = module.local.config.key_vault_name
  probes                        = module.local.config.quarkus_health_probes
  tags                          = module.local.config.tags
}
