package it.pagopa.selfcare.external_api.client;

import it.pagopa.selfcare.external_api.client.config.MsRegistryProxyNationalRegistriesClientConfig;
import it.pagopa.selfcare.registry_proxy.generated.openapi.v1.api.NationalRegistriesApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${rest-client.ms-registry-proxy-national-registries.serviceCode}", url = "${rest-client.ms-party-registry-proxy.base-url}", configuration = MsRegistryProxyNationalRegistriesClientConfig.class)
public interface MsRegistryProxyNationalRegistryRestClient extends NationalRegistriesApi {
}
