locals {
  # general
  app       = "Selfcare"
  prefix    = "selc"
  env_short = "p"
  env       = "prod"
  location  = "westeurope"
  domain    = "infra"

  storage_state = {
    resource_group_name  = "io-infra-rg"
    storage_account_name = "selcpstinfraterraform"
    container_name       = "azurermstate"
    key                  = "selc.infra.bootstrap.prod.tfstate"
  }

  storage_role = {
    name = "Storage Blob Data Contributor"
  }

  tags = {
    CreatedBy   = "Terraform"
    Environment = "Prod"
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
      subject    = "prod-ci"
    }
  ]

  cd_github_federations = [
    {
      repository = "selfcare"
      subject    = "prod-cd"
    }
  ]

  ci_github_federations_fe = [
    {
      repository = "selfcare-assistance-frontend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-pnpg-onboarding-frontend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-token-exchange-frontend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-login-frontend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-dashboard-frontend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-dashboard-admin-microfrontend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-dashboard-groups-microfrontend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-dashboard-users-microfrontend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-onboarding-frontend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-pnpg-dashboard-frontend"
      subject    = "prod-ci"
    }
  ]

  ci_github_federations_ms = [
    {
      repository = "selfcare-dashboard-backend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-external-api-backend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-infra"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-ms-core"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-ms-external-interceptor"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-ms-party-registry-proxy"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-onboarding"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-onboarding-backend"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-user"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-institution"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare-infra-private"
      subject    = "prod-ci"
    },
    {
      repository = "selfcare"
      subject    = "prod-ci"
    }
  ]

  cd_github_federations_fe = [
    {
      repository = "selfcare-assistance-frontend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-pnpg-onboarding-frontend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-login-frontend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-token-exchange-frontend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-dashboard-frontend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-dashboard-admin-microfrontend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-dashboard-groups-microfrontend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-dashboard-users-microfrontend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-onboarding-frontend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-pnpg-dashboard-frontend"
      subject    = "prod-cd"
    }
  ]

  cd_github_federations_ms = [
    {
      repository = "selfcare-dashboard-backend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-external-api-backend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-ms-core"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-ms-external-interceptor"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-ms-party-registry-proxy"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-onboarding"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-onboarding-backend"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-user"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-institution"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare-infra-private"
      subject    = "prod-cd"
    },
    {
      repository = "selfcare"
      subject    = "prod-cd"
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
      "selc-u-aks-rg" = [
        "Azure Kubernetes Service Cluster Admin Role"
      ],
      "selc-u-documents-storage-rg" = [
        "Storage Blob Data Contributor"
      ],
      "selc-u-logs-storage-rg" = [
        "Storage Blob Data Contributor"
      ]
    }
  }

  environment_cd_roles = {
    subscription = [
      "Contributor"
    ]
    resource_groups = {
      "selc-u-aks-rg" = [
        "Azure Kubernetes Service Cluster Admin Role"
      ],
      "selc-u-cosmosdb-mongodb-rg" : [
        "PagoPA Resource Lock Contributor"
      ],
      "selc-u-documents-storage-rg" = [
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
      "selc-u-contracts-storage-rg" = [
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
    "selfcare-assistance-frontend"            = "prod"
    "selfcare-pnpg-onboarding-frontend"       = "prod"
    "selfcare-token-exchange-frontend"        = "prod"
    "selfcare-login-frontend"                 = "prod"
    "selfcare-dashboard-frontend"             = "prod"
    "selfcare-dashboard-admin-microfrontend"  = "prod"
    "selfcare-dashboard-groups-microfrontend" = "prod"
    "selfcare-dashboard-users-microfrontend"  = "prod"
    "selfcare-onboarding-frontend"            = "prod"
    "selfcare-pnpg-dashboard-frontend"        = "prod"
  }
  github_federations_ms = {
    "selfcare"                         = "prod"
    "selfcare-dashboard-backend"       = "prod"
    "selfcare-external-api-backend"    = "prod"
    "selfcare-infra"                   = "prod"
    "selfcare-institution"             = "prod"
    "selfcare-ms-external-interceptor" = "prod"
    "selfcare-ms-party-registry-proxy" = "prod"
    "selfcare-onboarding-backend"      = "prod"
    "selfcare-user"                    = "prod"
  }
}
