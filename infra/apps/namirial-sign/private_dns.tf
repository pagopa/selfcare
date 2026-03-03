resource "azurerm_private_dns_a_record" "private_dns_record_a_azurecontainerapps_io" {
  count               = var.enable_ca_sws ? 1 : 0
  name                = "${azapi_resource.namirial_container_app[count.index].name}.${trimsuffix(data.azurerm_container_app_environment.container_app_environment.default_domain, ".${local.container_app_environment_dns_zone_name}")}"
  zone_name           = data.azurerm_private_dns_zone.private_azurecontainerapps_io.name
  resource_group_name = data.azurerm_resource_group.rg_vnet.name
  ttl                 = 3600
  records             = [data.azurerm_container_app_environment.container_app_environment.static_ip_address]
}
