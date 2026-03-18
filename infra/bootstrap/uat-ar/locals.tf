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
    storage_account_name = "selcustinfraterraform"
    container_name       = "azurermstate"
    key                  = "selc.infra.bootstrap.uat.tfstate"
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
      subject    = "uat-ci"
    }
  ]

  cd_github_federations = [
    {
      repository = "selfcare"
      subject    = "uat-cd"
    }
  ]

  ci_github_federations_fe = [
    {
      repository = "selfcare-assistance-frontend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-pnpg-onboarding-frontend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-token-exchange-frontend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-login-frontend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-dashboard-frontend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-dashboard-admin-microfrontend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-dashboard-groups-microfrontend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-dashboard-users-microfrontend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-onboarding-frontend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-pnpg-dashboard-frontend"
      subject    = "uat-ci"
    }
  ]

  ci_github_federations_ms = [
    {
      repository = "selfcare-dashboard-backend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-external-api-backend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-infra"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-ms-core"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-ms-external-interceptor"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-ms-party-registry-proxy"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-onboarding"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-onboarding-backend"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-user"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-institution"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare-infra-private"
      subject    = "uat-ci"
    },
    {
      repository = "selfcare"
      subject    = "uat-ci"
    }
  ]

  cd_github_federations_fe = [
    {
      repository = "selfcare-assistance-frontend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-pnpg-onboarding-frontend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-login-frontend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-token-exchange-frontend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-dashboard-frontend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-dashboard-admin-microfrontend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-dashboard-groups-microfrontend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-dashboard-users-microfrontend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-onboarding-frontend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-pnpg-dashboard-frontend"
      subject    = "uat-cd"
    }
  ]

  cd_github_federations_ms = [
    {
      repository = "selfcare-dashboard-backend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-external-api-backend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-ms-core"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-ms-external-interceptor"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-ms-party-registry-proxy"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-onboarding"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-onboarding-backend"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-user"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-institution"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare-infra-private"
      subject    = "uat-cd"
    },
    {
      repository = "selfcare"
      subject    = "uat-cd"
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
    "selfcare-assistance-frontend"            = "uat"
    "selfcare-pnpg-onboarding-frontend"       = "uat"
    "selfcare-token-exchange-frontend"        = "uat"
    "selfcare-login-frontend"                 = "uat"
    "selfcare-dashboard-frontend"             = "uat"
    "selfcare-dashboard-admin-microfrontend"  = "uat"
    "selfcare-dashboard-groups-microfrontend" = "uat"
    "selfcare-dashboard-users-microfrontend"  = "uat"
    "selfcare-onboarding-frontend"            = "uat"
    "selfcare-pnpg-dashboard-frontend"        = "uat"
  }
  github_federations_ms = {
    "selfcare-dashboard-backend"       = "uat"
    "selfcare-external-api-backend"    = "uat"
    "selfcare-infra"                   = "uat"
    "selfcare-institution"             = "uat"
    "selfcare-ms-external-interceptor" = "uat"
    "selfcare-ms-party-registry-proxy" = "uat"
    "selfcare-onboarding-backend"      = "uat"
    "selfcare-user"                    = "uat"
  }
}
