mock_provider "azurerm" {
  mock_data "azurerm_container_app_environment" {
    defaults = {
      id                             = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/test-rg/providers/Microsoft.App/managedEnvironments/test-cae"
      default_domain                 = "test.westeurope.azurecontainerapps.io"
      static_ip_address              = "10.1.0.4"
      internal_load_balancer_enabled = true
    }
  }

  mock_data "azurerm_subnet" {
    defaults = {
      address_prefixes = ["10.1.144.0/24"]
    }
  }
}

variables {
  name                           = "test-auth-m2m-proxy-ca"
  resource_group_name            = "test-rg"
  container_app_environment_name = "test-cae"
  vnet_resource_group_name       = "test-vnet-rg"
  virtual_network_name           = "test-vnet"
  function_subnet_name           = "test-onboarding-fn-snet"
  auth_container_app_name        = "test-auth-ms-ca"
  image                          = "ghcr.io/pagopa/selfcare-auth-ms:sha-1234567"
  tags                           = {}
}

run "restrict_ingress_and_target_auth" {
  command = plan

  assert {
    condition = (
      azurerm_container_app.proxy.ingress[0].external_enabled &&
      azurerm_container_app.proxy.ingress[0].allow_insecure_connections &&
      azurerm_container_app.proxy.ingress[0].target_port == 8080 &&
      length(azurerm_container_app.proxy.ingress[0].ip_security_restriction) == 1 &&
      one(azurerm_container_app.proxy.ingress[0].ip_security_restriction).action == "Allow" &&
      one(azurerm_container_app.proxy.ingress[0].ip_security_restriction).ip_address_range == "10.1.144.0/24"
    )
    error_message = "The proxy must allow only the Function subnet on its private HTTP ingress."
  }

  assert {
    condition = (
      azurerm_container_app.proxy.template[0].container[0].image == var.image &&
      contains(azurerm_container_app.proxy.template[0].container[0].args, "it.pagopa.selfcare.auth.proxy.MachineTokenProxy") &&
      one(azurerm_container_app.proxy.template[0].container[0].env).value == "http://test-auth-ms-ca:8082/token/service"
    )
    error_message = "The proxy must reuse auth's image and target only its internal M2M endpoint."
  }

  assert {
    condition = (
      azurerm_private_dns_a_record.proxy.name == "test-auth-m2m-proxy-ca.test.westeurope" &&
      length(azurerm_private_dns_a_record.proxy.records) == 1 &&
      contains(azurerm_private_dns_a_record.proxy.records, "10.1.0.4")
    )
    error_message = "The proxy DNS record must resolve to the private CAE address."
  }
}

run "reject_empty_allowlist" {
  command = plan

  override_data {
    target = data.azurerm_subnet.function
    values = {
      address_prefixes = []
    }
  }

  expect_failures = [azurerm_container_app.proxy]
}

run "allow_all_function_prefixes" {
  command = plan

  override_data {
    target = data.azurerm_subnet.function
    values = {
      address_prefixes = ["10.1.145.0/24", "10.1.144.0/24"]
    }
  }

  assert {
    condition = (
      length(azurerm_container_app.proxy.ingress[0].ip_security_restriction) == 2 &&
      alltrue([
        for rule in azurerm_container_app.proxy.ingress[0].ip_security_restriction :
        rule.action == "Allow" && contains(["10.1.144.0/24", "10.1.145.0/24"], rule.ip_address_range)
      ])
    )
    error_message = "Every Function subnet prefix must be allowed, with no extra ranges."
  }
}

run "reject_unrestricted_allowlist" {
  command = plan

  override_data {
    target = data.azurerm_subnet.function
    values = {
      address_prefixes = ["0.0.0.0/0"]
    }
  }

  expect_failures = [azurerm_container_app.proxy]
}

run "reject_ipv6_allowlist" {
  command = plan

  override_data {
    target = data.azurerm_subnet.function
    values = {
      address_prefixes = ["fd00::/64"]
    }
  }

  expect_failures = [azurerm_container_app.proxy]
}

run "reject_public_environment" {
  command = plan

  override_data {
    target = data.azurerm_container_app_environment.environment
    values = {
      internal_load_balancer_enabled = false
    }
  }

  expect_failures = [azurerm_container_app.proxy]
}
