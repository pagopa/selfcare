mock_provider "azurerm" {
  override_during = plan

  mock_resource "azurerm_key_vault" {
    defaults = {
      id = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/security-rg/providers/Microsoft.KeyVault/vaults/test-kv"
    }
  }
}

variables {
  prefix              = "selc"
  domain              = "ar"
  location            = "westeurope"
  resource_group_name = "security-rg"
  tenant_id           = "00000000-0000-0000-0000-000000000001"
  networking = {
    resource_group_name        = "network-rg"
    virtual_network_id         = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/network-rg/providers/Microsoft.Network/virtualNetworks/test-vnet"
    private_endpoint_subnet_id = "/subscriptions/00000000-0000-0000-0000-000000000001/resourceGroups/network-rg/providers/Microsoft.Network/virtualNetworks/test-vnet/subnets/endpoints"
  }
  tags = { Environment = "Test" }
}

run "assigns_configured_group_roles" {
  command = plan

  variables {
    env_short = "d"
    adgroup_admin = {
      object_id            = "00000000-0000-0000-0000-000000000002"
      role_definition_name = "Key Vault Secrets Officer"
    }
    adgroup_developers = {
      object_id            = "00000000-0000-0000-0000-000000000003"
      role_definition_name = "Key Vault Secrets Officer"
    }
  }

  assert {
    condition = (
      azurerm_role_assignment.admin_secrets.role_definition_name == "Key Vault Secrets Officer" &&
      azurerm_role_assignment.developers_secrets.role_definition_name == "Key Vault Secrets Officer"
    )
    error_message = "Vault role assignments must use the externally configured role_definition_name."
  }

  assert {
    condition = (
      azurerm_role_assignment.admin_secrets.scope == azurerm_key_vault.this.id &&
      azurerm_role_assignment.developers_secrets.scope == azurerm_key_vault.this.id &&
      azurerm_role_assignment.admin_secrets.principal_id == var.adgroup_admin.object_id &&
      azurerm_role_assignment.developers_secrets.principal_id == var.adgroup_developers.object_id
    )
    error_message = "Each group must receive its own role scoped only to the new vault."
  }
}

run "supports_distinct_external_roles_per_group" {
  command = plan

  variables {
    env_short = "u"
    adgroup_admin = {
      object_id            = "00000000-0000-0000-0000-000000000002"
      role_definition_name = "Key Vault Administrator"
    }
    adgroup_developers = {
      object_id            = "00000000-0000-0000-0000-000000000003"
      role_definition_name = "Key Vault Secrets User"
    }
  }

  assert {
    condition = (
      azurerm_role_assignment.admin_secrets.role_definition_name == "Key Vault Administrator" &&
      azurerm_role_assignment.developers_secrets.role_definition_name == "Key Vault Secrets User"
    )
    error_message = "Admin and developer groups must independently honor any externally passed role_definition_name."
  }
}
