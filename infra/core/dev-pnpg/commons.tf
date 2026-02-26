###############################################################################
# Azure Group
###############################################################################

module "azure_group" {
  source = "../_modules/data/azure_group"

  prefix    = local.prefix
  env_short = local.env_short
}