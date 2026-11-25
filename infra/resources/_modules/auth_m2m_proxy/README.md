# Auth M2M Function proxy

This module deploys the auth image as a separate HTTP proxy in an internal Container
Apps environment. Ingress allows only the onboarding Function integration subnet,
and private DNS resolves the proxy to the environment's internal load balancer.
The proxy forwards only `POST /token/service` to the local auth port 8082.

The allowlist authenticates a subnet, not an individual caller or tenant. HTTP
does not encrypt session tokens. See the [M2M rollout guide](../../../../libs/selfcare-sdk-m2m/README.md)
for the trust boundary, deployment order and required connectivity checks.

<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.6.0 |
| <a name="requirement_azurerm"></a> [azurerm](#requirement\_azurerm) | >= 4.0, < 5.0 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | 4.81.0 |

## Modules

No modules.

## Resources

| Name | Type |
| ---- | ---- |
| [azurerm_container_app.proxy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/container_app) | resource |
| [azurerm_private_dns_a_record.proxy](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/private_dns_a_record) | resource |
| [azurerm_container_app_environment.environment](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/container_app_environment) | data source |
| [azurerm_private_dns_zone.container_apps](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/private_dns_zone) | data source |
| [azurerm_subnet.function](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/subnet) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_auth_container_app_name"></a> [auth\_container\_app\_name](#input\_auth\_container\_app\_name) | Name of the auth Container App in the same environment. | `string` | n/a | yes |
| <a name="input_container_app_environment_name"></a> [container\_app\_environment\_name](#input\_container\_app\_environment\_name) | Name of the internal environment shared with auth. | `string` | n/a | yes |
| <a name="input_function_subnet_name"></a> [function\_subnet\_name](#input\_function\_subnet\_name) | Onboarding Function integration subnet whose IPv4 prefixes are allowed. | `string` | n/a | yes |
| <a name="input_image"></a> [image](#input\_image) | Auth image containing the standalone proxy entry point. | `string` | n/a | yes |
| <a name="input_name"></a> [name](#input\_name) | Name of the proxy Container App. | `string` | n/a | yes |
| <a name="input_resource_group_name"></a> [resource\_group\_name](#input\_resource\_group\_name) | Resource group containing the internal Container Apps environment. | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | Tags assigned to the proxy Container App. | `map(string)` | n/a | yes |
| <a name="input_virtual_network_name"></a> [virtual\_network\_name](#input\_virtual\_network\_name) | Virtual network containing the Function integration subnet. | `string` | n/a | yes |
| <a name="input_vnet_resource_group_name"></a> [vnet\_resource\_group\_name](#input\_vnet\_resource\_group\_name) | Resource group containing the virtual network and private DNS zone. | `string` | n/a | yes |

## Outputs

| Name | Description |
| ---- | ----------- |
| <a name="output_endpoint"></a> [endpoint](#output\_endpoint) | Private HTTP endpoint to configure on the onboarding Function. |
<!-- END_TF_DOCS -->
