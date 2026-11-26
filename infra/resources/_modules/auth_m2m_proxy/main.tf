terraform {
  required_version = ">= 1.6.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 4.0, < 5.0"
    }
  }
}

data "azurerm_container_app_environment" "environment" {
  name                = var.container_app_environment_name
  resource_group_name = var.resource_group_name
}

data "azurerm_subnet" "function" {
  name                 = var.function_subnet_name
  virtual_network_name = var.virtual_network_name
  resource_group_name  = var.vnet_resource_group_name
}

data "azurerm_private_dns_zone" "container_apps" {
  name                = "azurecontainerapps.io"
  resource_group_name = var.vnet_resource_group_name
}

resource "azurerm_container_app" "proxy" {
  name                         = var.name
  container_app_environment_id = data.azurerm_container_app_environment.environment.id
  resource_group_name          = var.resource_group_name
  revision_mode                = "Single"
  workload_profile_name        = "Consumption"
  tags                         = var.tags

  ingress {
    external_enabled           = true
    allow_insecure_connections = true
    target_port                = 8080
    transport                  = "http"

    dynamic "ip_security_restriction" {
      for_each = {
        for index, cidr in sort(tolist(data.azurerm_subnet.function.address_prefixes)) :
        tostring(index) => cidr
      }
      content {
        name             = "function-subnet-${ip_security_restriction.key}"
        ip_address_range = ip_security_restriction.value
        action           = "Allow"
      }
    }

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas = 1
    max_replicas = 2

    container {
      name   = "m2m-proxy"
      image  = var.image
      cpu    = 0.25
      memory = "0.5Gi"
      command = [
        "java"
      ]
      args = [
        "-Xms32m",
        "-Xmx128m",
        "-cp",
        "/app/app/*",
        "it.pagopa.selfcare.auth.proxy.MachineTokenProxy"
      ]

      env {
        name  = "M2M_PROXY_UPSTREAM_URL"
        value = "http://${var.auth_container_app_name}:8082/token/service"
      }

      liveness_probe {
        transport = "TCP"
        port      = 8080
      }

      readiness_probe {
        transport = "TCP"
        port      = 8080
      }
    }
  }

  lifecycle {
    precondition {
      condition     = data.azurerm_container_app_environment.environment.internal_load_balancer_enabled
      error_message = "The M2M proxy requires an internal Container Apps environment."
    }
    precondition {
      condition = length(data.azurerm_subnet.function.address_prefixes) > 0 && alltrue([
        for cidr in data.azurerm_subnet.function.address_prefixes :
        can(cidrnetmask(cidr)) && cidr != "0.0.0.0/0"
      ])
      error_message = "The Function subnet must have at least one restricted IPv4 address prefix."
    }
  }
}

resource "azurerm_private_dns_a_record" "proxy" {
  name = "${azurerm_container_app.proxy.name}.${trimsuffix(
    data.azurerm_container_app_environment.environment.default_domain,
    ".${data.azurerm_private_dns_zone.container_apps.name}"
  )}"
  zone_name           = data.azurerm_private_dns_zone.container_apps.name
  resource_group_name = var.vnet_resource_group_name
  ttl                 = 3600
  records             = [data.azurerm_container_app_environment.environment.static_ip_address]
}

output "endpoint" {
  description = "Private HTTP endpoint to configure on the onboarding Function."
  value       = "http://${azurerm_container_app.proxy.ingress[0].fqdn}/token/service"
}
