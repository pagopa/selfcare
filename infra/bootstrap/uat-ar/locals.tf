locals {
  # general
  app       = "Selfcare"
  prefix    = "selc"
  env_short = "u"
  env       = "uat"
  location  = "westeurope"
  domain    = "infra"

  storage_state = {
    resource_group_name  = "io-infra-rg"
    storage_account_name = "selc${local.env_short}stinfraterraform"
    container_name       = "azurermstate"
    key                  = "selc.infra.bootstrap.${local.env}.tfstate"
  }

  storage_role = {
    name = "Storage Blob Data Contributor"
  }

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Uat"
    Owner       = "SelfCare"
    Source      = "https://github.com/pagopa/selfcare"
    CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
  }

  project = "${local.prefix}-${local.env_short}"

  github = {
    org        = "pagopa"
    repository = "selfcare"

    ci_branch_policy_enabled = local.github_repository_environment_ci.protected_branches == true || local.github_repository_environment_ci.custom_branch_policies == true
    cd_branch_policy_enabled = local.github_repository_environment_cd.protected_branches == true || local.github_repository_environment_cd.custom_branch_policies == true
  }

  env_ci_secrets = {
    "AZURE_CLIENT_ID_CI"    = module.identity_ci.identity_client_id
    "AZURE_SUBSCRIPTION_ID" = data.azurerm_client_config.current.subscription_id
    "AZURE_TENANT_ID"       = data.azurerm_client_config.current.tenant_id,
    "ARM_CLIENT_ID_CI"      = module.identity_ci.identity_client_id
    "ARM_SUBSCRIPTION_ID"   = data.azurerm_client_config.current.subscription_id
    "ARM_TENANT_ID"         = data.azurerm_client_config.current.tenant_id,
  }

  env_cd_secrets = {
    "AZURE_CLIENT_ID_CD"    = module.identity_cd.identity_client_id
    "AZURE_SUBSCRIPTION_ID" = data.azurerm_client_config.current.subscription_id
    "AZURE_TENANT_ID"       = data.azurerm_client_config.current.tenant_id,
    "ARM_CLIENT_ID_CD"      = module.identity_cd.identity_client_id
    "ARM_SUBSCRIPTION_ID"   = data.azurerm_client_config.current.subscription_id
    "ARM_TENANT_ID"         = data.azurerm_client_config.current.tenant_id,
  }

  ci_github_federations = [
    {
      repository = "selfcare"
      subject    = "${local.env}-ci"
    }
  ]

  cd_github_federations = [
    {
      repository = "selfcare"
      subject    = "${local.env}-cd"
    }
  ]

  ci_github_federations_fe = [
    {
      repository = "selfcare-assistance-frontend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-pnpg-onboarding-frontend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-token-exchange-frontend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-login-frontend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-dashboard-frontend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-dashboard-admin-microfrontend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-dashboard-groups-microfrontend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-dashboard-users-microfrontend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-onboarding-frontend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-pnpg-dashboard-frontend"
      subject    = "${local.env}-ci"
    }
  ]

  ci_github_federations_ms = [
    {
      repository = "selfcare-dashboard-backend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-external-api-backend"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-ms-core"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-user"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare-institution"
      subject    = "${local.env}-ci"
    },
    {
      repository = "selfcare"
      subject    = "${local.env}-ci"
    }
  ]

  cd_github_federations_fe = [
    {
      repository = "selfcare-assistance-frontend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-pnpg-onboarding-frontend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-login-frontend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-token-exchange-frontend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-dashboard-frontend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-dashboard-admin-microfrontend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-dashboard-groups-microfrontend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-dashboard-users-microfrontend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-onboarding-frontend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-pnpg-dashboard-frontend"
      subject    = "${local.env}-cd"
    }
  ]

  cd_github_federations_ms = [
    {
      repository = "selfcare-dashboard-backend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-external-api-backend"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-ms-core"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-user"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare-institution"
      subject    = "${local.env}-cd"
    },
    {
      repository = "selfcare"
      subject    = "${local.env}-cd"
    }
  ]

  environment_ci_roles = {
    subscription = [
      "Reader",
      "PagoPA IaC Reader",
      "Reader and Data Access"
    ]
    resource_groups = {
      "terraform-state-rg" = [
        "Storage Blob Data Contributor"
      ],
      "io-infra-rg" = [
        "Storage Blob Data Contributor"
      ],
      "selc-${local.env_short}-aks-rg" = [
        "Azure Kubernetes Service Cluster Admin Role"
      ],
      "selc-${local.env_short}-documents-storage-rg" = [
        "Storage Blob Data Contributor"
      ],
      "selc-${local.env_short}-logs-storage-rg" = [
        "Storage Blob Data Contributor"
      ],
      "selc-${local.env_short}-checkout-fe-rg" = [
        "Storage Blob Data Contributor"
      ],
      "selc-${local.env_short}-cosmosdb-mongodb-rg" = [
        "DocumentDB Account Contributor",
        "Cosmos DB Account Reader Role"
      ],
      "selc-${local.env_short}-pnpg-spid-testenv-rg" = [
        "Storage Account Key Operator Service Role"
      ],
      "selc-${local.env_short}-weu-pnpg-cosmosdb-mongodb-rg" = [
        "DocumentDB Account Contributor"
      ]
    }
  }

  environment_cd_roles = {
    subscription = [
      "Contributor"
    ]
    resource_groups = {
      "selc-${local.env_short}-aks-rg" = [
        "Azure Kubernetes Service Cluster Admin Role"
      ],
      "selc-${local.env_short}-cosmosdb-mongodb-rg" : [
        "PagoPA Resource Lock Contributor"
      ],
      "selc-${local.env_short}-documents-storage-rg" = [
        "Storage Blob Data Contributor"
      ],
      "io-infra-rg" = [
        "Storage Blob Data Contributor"
      ]
    }
  }

  environment_ci_roles_ms = {
    subscription = [
      "Reader",
      "PagoPA IaC Reader",
      "ContainerApp Reader"
    ]
    resource_groups = {
      terraform-state-rg = [
        "Storage Blob Data Contributor"
      ],
      "selc-${local.env_short}-contracts-storage-rg" = [
        "Storage Blob Data Contributor"
      ],
      io-infra-rg = [
        "Storage Blob Data Contributor"
      ],
    }
  }

  environment_cd_roles_ms = {
    subscription = [
      "Contributor"
    ]
    resource_groups = {
      terraform-state-rg = [
        "Storage Blob Data Contributor"
      ],
      io-infra-rg = [
        "Storage Blob Data Contributor"
      ],
    }
  }

  github_repository_environment_ci = {
    protected_branches     = false
    custom_branch_policies = false
    reviewers_teams        = []
    branch_pattern         = null
  }

  github_repository_environment_cd = {
    protected_branches     = false
    custom_branch_policies = false
    reviewers_teams        = []
    branch_pattern         = null
  }

  github_federations_fe = {
    "selfcare-assistance-frontend"            = "${local.env}"
    "selfcare-pnpg-onboarding-frontend"       = "${local.env}"
    "selfcare-token-exchange-frontend"        = "${local.env}"
    "selfcare-login-frontend"                 = "${local.env}"
    "selfcare-dashboard-frontend"             = "${local.env}"
    "selfcare-dashboard-admin-microfrontend"  = "${local.env}"
    "selfcare-dashboard-groups-microfrontend" = "${local.env}"
    "selfcare-dashboard-users-microfrontend"  = "${local.env}"
    "selfcare-onboarding-frontend"            = "${local.env}"
    "selfcare-pnpg-dashboard-frontend"        = "${local.env}"
  }
  github_federations_ms = {
    "selfcare"                         = "${local.env}"
    "selfcare-dashboard-backend"       = "${local.env}"
    "selfcare-external-api-backend"    = "${local.env}"
    "selfcare-infra"                   = "${local.env}"
    "selfcare-institution"             = "${local.env}"
    "selfcare-ms-external-interceptor" = "${local.env}"
    "selfcare-ms-party-registry-proxy" = "${local.env}"
    "selfcare-onboarding-backend"      = "${local.env}"
    "selfcare-user"                    = "${local.env}"
  }
}
