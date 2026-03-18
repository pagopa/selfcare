locals {
  # general
  app       = "Selfcare"
  prefix    = "selc"
  env_short = "d"
  env       = "dev"
  location  = "westeurope"
  domain    = "infra"

  storage_state = {
    resource_group_name  = "io-infra-rg"
    storage_account_name = "selcdstinfraterraform"
    container_name       = "azurermstate"
    key                  = "selc.infra.bootstrap.dev.tfstate"
  }

  storage_role = {
    name = "Storage Blob Data Contributor"
  }

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Dev"
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
      subject    = "dev-ci"
    }
  ]

  cd_github_federations = [
    {
      repository = "selfcare"
      subject    = "dev-cd"
    }
  ]

  ci_github_federations_fe = [
    {
      repository = "selfcare-assistance-frontend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-pnpg-onboarding-frontend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-token-exchange-frontend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-login-frontend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-dashboard-frontend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-dashboard-admin-microfrontend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-dashboard-groups-microfrontend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-dashboard-users-microfrontend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-onboarding-frontend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-pnpg-dashboard-frontend"
      subject    = "dev-ci"
    }
  ]

  ci_github_federations_ms = [
    {
      repository = "selfcare-dashboard-backend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-external-api-backend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-infra"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-ms-core"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-ms-external-interceptor"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-ms-party-registry-proxy"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-onboarding"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-onboarding-backend"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-user"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-institution"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare-infra-private"
      subject    = "dev-ci"
    },
    {
      repository = "selfcare"
      subject    = "dev-ci"
    }
  ]

  cd_github_federations_fe = [
    {
      repository = "selfcare-assistance-frontend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-pnpg-onboarding-frontend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-login-frontend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-token-exchange-frontend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-dashboard-frontend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-dashboard-admin-microfrontend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-dashboard-groups-microfrontend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-dashboard-users-microfrontend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-onboarding-frontend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-pnpg-dashboard-frontend"
      subject    = "dev-cd"
    }
  ]

  cd_github_federations_ms = [
    {
      repository = "selfcare-dashboard-backend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-external-api-backend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-ms-core"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-ms-external-interceptor"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-ms-party-registry-proxy"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-onboarding"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-onboarding-backend"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-user"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-institution"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare-infra-private"
      subject    = "dev-cd"
    },
    {
      repository = "selfcare"
      subject    = "dev-cd"
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
      "selc-d-aks-rg" = [
        "Azure Kubernetes Service Cluster Admin Role"
      ],
      "selc-d-documents-storage-rg" = [
        "Storage Blob Data Contributor"
      ],
      "selc-d-logs-storage-rg" = [
        "Storage Blob Data Contributor"
      ]
    }
  }

  environment_cd_roles = {
    subscription = [
      "Contributor"
    ]
    resource_groups = {
      "selc-d-aks-rg" = [
        "Azure Kubernetes Service Cluster Admin Role"
      ],
      "selc-d-cosmosdb-mongodb-rg" : [
        "PagoPA Resource Lock Contributor"
      ],
      "selc-d-documents-storage-rg" = [
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
      "selc-d-contracts-storage-rg" = [
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
    "selfcare-assistance-frontend"            = "dev"
    "selfcare-pnpg-onboarding-frontend"       = "dev"
    "selfcare-token-exchange-frontend"        = "dev"
    "selfcare-login-frontend"                 = "dev"
    "selfcare-dashboard-frontend"             = "dev"
    "selfcare-dashboard-admin-microfrontend"  = "dev"
    "selfcare-dashboard-groups-microfrontend" = "dev"
    "selfcare-dashboard-users-microfrontend"  = "dev"
    "selfcare-onboarding-frontend"            = "dev"
    "selfcare-pnpg-dashboard-frontend"        = "dev"
  }
  github_federations_ms = {
    "selfcare-dashboard-backend"       = "dev"
    "selfcare-external-api-backend"    = "dev"
    "selfcare-institution"             = "dev"
    "selfcare-ms-external-interceptor" = "dev"
    "selfcare-ms-party-registry-proxy" = "dev"
    "selfcare-onboarding-backend"      = "dev"
    "selfcare-user"                    = "dev"
  }
}
