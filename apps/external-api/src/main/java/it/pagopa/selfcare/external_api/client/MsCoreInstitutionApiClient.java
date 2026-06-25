package it.pagopa.selfcare.external_api.client;

import it.pagopa.selfcare.core.generated.openapi.v1.api.InstitutionApi;
import it.pagopa.selfcare.external_api.client.config.MsCoreRestClientConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Ms Core Rest Client
 */
@FeignClient(name = "${rest-client.ms-core-institution-api.serviceCode}", url = "${rest-client.ms-core.base-url}", configuration = MsCoreRestClientConfig.class)
public interface MsCoreInstitutionApiClient extends InstitutionApi {
}
