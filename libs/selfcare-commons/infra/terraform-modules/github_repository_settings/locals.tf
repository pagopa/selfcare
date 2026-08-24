locals {
  project = "selc-%s"

  # cannot be moved below because of circular references
  dev_alias  = "d"
  uat_alias  = "u"
  prod_alias = "p"

  subscription_id_dev  = "1ab5e788-3b98-4c63-bd05-de0c7388c853"
  subscription_id_uat  = "f47d50dc-b874-4e04-9d5c-c27f5053a651"
  subscription_id_prod = "813119d7-0943-46ed-8ebe-cebe24f9106c"

  key_vault_resource_group_name = "${local.project}-sec-rg"
  key_vault_name                = "${local.project}-kv"

  identity_resource_group_name = "${local.project}-identity-rg"
  identity_ci_name             = "${local.project}-ms-github-ci-identity"
  identity_cd_name             = "${local.project}-ms-github-cd-identity"

  identity_ci_fe_name = "${local.project}-fe-github-ci-identity"
  identity_cd_fe_name = "${local.project}-fe-github-cd-identity"

  repo_variables = {
    "ARM_TENANT_ID" = data.azurerm_client_config.current.tenant_id,
  }

  repo_secrets = {
    "SONAR_TOKEN"      = data.azurerm_key_vault_secret.key_vault_sonar.value,
    "GH_PAT_VARIABLES" = data.azurerm_key_vault_secret.key_github_path_token.value,
  }

  dev = {
    ci = {
      protected_branches     = false
      custom_branch_policies = false
      variables = {
        "ARM_SUBSCRIPTION_ID" = local.subscription_id_dev
      }
      secrets = {
        "ARM_CLIENT_ID" = var.identity_component == "ms" ? data.azurerm_user_assigned_identity.identity_dev_ci.client_id : data.azurerm_user_assigned_identity.identity_dev_fe_ci.client_id
      }
    }
    cd = {
      protected_branches     = false
      custom_branch_policies = false
      variables = {
        "ARM_SUBSCRIPTION_ID" = local.subscription_id_dev
      }
      secrets = {
        "ARM_CLIENT_ID" = var.identity_component == "ms" ? data.azurerm_user_assigned_identity.identity_dev_cd.client_id : data.azurerm_user_assigned_identity.identity_dev_fe_cd.client_id
      }
    }
  }

  uat = {
    ci = {
      protected_branches     = false
      custom_branch_policies = true
      reviewers_teams        = ["selfcare-contributors"]
      branch_pattern         = "releases/*"
      variables = {
        "ARM_SUBSCRIPTION_ID" = local.subscription_id_uat
      }
      secrets = {
        "ARM_CLIENT_ID" = var.identity_component == "ms" ? data.azurerm_user_assigned_identity.identity_uat_ci.client_id : data.azurerm_user_assigned_identity.identity_uat_fe_ci.client_id
      }
    }
    cd = {
      protected_branches     = false
      custom_branch_policies = true
      reviewers_teams        = ["selfcare-contributors"]
      branch_pattern         = "releases/*"
      variables = {
        "ARM_SUBSCRIPTION_ID" = local.subscription_id_uat
      }
      secrets = {
        "ARM_CLIENT_ID" = var.identity_component == "ms" ? data.azurerm_user_assigned_identity.identity_uat_cd.client_id : data.azurerm_user_assigned_identity.identity_uat_fe_cd.client_id
      }
    }
  }

  prod = {
    ci = {
      protected_branches     = false
      custom_branch_policies = true
      reviewers_teams        = ["selfcare-contributors"]
      branch_pattern         = "releases/*"
      variables = {
        "ARM_SUBSCRIPTION_ID" = local.subscription_id_prod
      }
      secrets = {
        "ARM_CLIENT_ID" = var.identity_component == "ms" ? data.azurerm_user_assigned_identity.identity_prod_ci.client_id : data.azurerm_user_assigned_identity.identity_prod_fe_ci.client_id
      }
    }
    cd = {
      protected_branches     = false
      custom_branch_policies = true
      reviewers_teams        = ["selfcare-contributors"]
      branch_pattern         = "releases/*"
      variables = {
        "ARM_SUBSCRIPTION_ID" = local.subscription_id_prod
      }
      secrets = {
        "ARM_CLIENT_ID" = var.identity_component == "ms" ? data.azurerm_user_assigned_identity.identity_prod_cd.client_id : data.azurerm_user_assigned_identity.identity_prod_fe_cd.client_id
      }
    }
  }
}
