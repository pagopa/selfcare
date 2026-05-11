terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "<= 3.106.0"
    }
  }

  backend "azurerm" {}
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
}

module "container_app_job_selfhosted_runner" {
  source = "github.com/pagopa/dx//infra/modules/github_selfhosted_runner_on_container_app_jobs?ref=main"

  prefix    = var.prefix
  env_short = var.env_short

  repo_name = local.repo_name

  container_app_job_name = "ext-api-be-infra"

  container_app_environment = {
    name                = "${local.selc_project}-github-runner-cae"
    resource_group_name = "${local.selc_project}-github-runner-rg"
  }

  key_vault = {
    resource_group_name = data.azurerm_key_vault.key_vault_common.resource_group_name
    name                = data.azurerm_key_vault.key_vault_common.name
    secret_name         = var.key_vault.pat_secret_name
  }

  tags = var.tags
}
