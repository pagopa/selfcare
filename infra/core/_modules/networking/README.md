# networking

<!-- BEGINNING OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
## Requirements

No requirements.

## Providers

| Name | Version |
|------|---------|
| <a name="provider_azurerm"></a> [azurerm](#provider\_azurerm) | n/a |

## Modules

No modules.

## Resources

| Name | Type |
|------|------|
| [azurerm_nat_gateway_public_ip_association.subnet_pip_nat_gateway](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/nat_gateway_public_ip_association) | resource |
| [azurerm_network_security_group.subnet_nsg](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/network_security_group) | resource |
| [azurerm_network_security_rule.cae_subnet_inbound_rule](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/network_security_rule) | resource |
| [azurerm_network_security_rule.cae_subnet_outbound_rule](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/network_security_rule) | resource |
| [azurerm_subnet.container_app_snet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/subnet) | resource |
| [azurerm_subnet_nat_gateway_association.subnet_gateway_association](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/subnet_nat_gateway_association) | resource |
| [azurerm_subnet_network_security_group_association.nsg_cae_subnet_association](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/subnet_network_security_group_association) | resource |
| [azurerm_nat_gateway.nat_gateway](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/nat_gateway) | data source |
| [azurerm_public_ip.pip_outbound](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/public_ip) | data source |
| [azurerm_virtual_network.vnet](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/data-sources/virtual_network) | data source |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_cidr_subnet_cae"></a> [cidr\_subnet\_cae](#input\_cidr\_subnet\_cae) | CIDR block for ContainerAppEnvironment subnet | `string` | n/a | yes |
| <a name="input_cidr_subnet_main"></a> [cidr\_subnet\_main](#input\_cidr\_subnet\_main) | CIDR block for main subnet | `string` | `null` | no |
| <a name="input_container_app_name_snet"></a> [container\_app\_name\_snet](#input\_container\_app\_name\_snet) | Name of container app subnet | `string` | n/a | yes |
| <a name="input_core_vnet"></a> [core\_vnet](#input\_core\_vnet) | True if the module define the core vnet, true for AR | `bool` | `true` | no |
| <a name="input_delegation"></a> [delegation](#input\_delegation) | Container app subnet delegation | <pre>list(object({<br/>    name                       = string<br/>    service_delegation_name    = string<br/>    service_delegation_actions = list(string)<br/>  }))</pre> | <pre>[<br/>  {<br/>    "name": "Microsoft.App/environments",<br/>    "service_delegation_actions": [<br/>      "Microsoft.Network/virtualNetworks/subnets/join/action"<br/>    ],<br/>    "service_delegation_name": "Microsoft.App/environments"<br/>  }<br/>]</pre> | no |
| <a name="input_project"></a> [project](#input\_project) | SelfCare prefix and short environment | `string` | n/a | yes |
| <a name="input_tags"></a> [tags](#input\_tags) | Resource tags | `map(any)` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_subnet"></a> [subnet](#output\_subnet) | n/a |
<!-- END OF PRE-COMMIT-TERRAFORM DOCS HOOK -->
