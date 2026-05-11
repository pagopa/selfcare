package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.backoffice.generated.openapi.v1.api.CreditorInstitutionsApi;
import it.pagopa.selfcare.dashboard.config.restclient.PagoPABackOfficeRestClientConfig;
import org.springframework.cloud.openfeign.FeignClient;


/**
 * PagoPa Ms Backoffice Rest Client
 */
@FeignClient(name = "${rest-client.pago-pa-backoffice-stations-api.serviceCode}", url = "${rest-client.pago-pa-backoffice.base-url}", configuration = PagoPABackOfficeRestClientConfig.class)
public interface MsBackOfficeStationApiClient extends CreditorInstitutionsApi {
}

