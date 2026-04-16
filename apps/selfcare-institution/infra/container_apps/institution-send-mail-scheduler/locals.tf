locals {
  prefix  = "selc"
  project = "selc-${var.env_short}"

  container_app_environment_name = "${local.project}-${var.cae_name}"
  ca_resource_group_name         = "${local.project}-container-app${var.suffix_increment}-rg"
  container_app_name             = "${local.project}-institution-send-mail-job"

  key_vault_resource_group_name = "${local.project}-sec-rg"
  key_vault_name                = "${local.project}-kv"
  image_name                    = "selfcare-institution-send-mail-scheduler"

  mongo_db = {
    mongodb_rg_name               = "${local.prefix}-${var.env_short}-cosmosdb-mongodb-rg",
    cosmosdb_account_mongodb_name = "${local.prefix}-${var.env_short}-cosmosdb-mongodb-account"
  }

  secrets_env = [for env, secret in var.secrets_names :
    {
      name      = env
      secretRef = secret
  }]
}