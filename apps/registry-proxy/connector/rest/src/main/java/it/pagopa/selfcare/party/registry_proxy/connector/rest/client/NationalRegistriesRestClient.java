package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.Businesses;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.LegalAddressRequest;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.NationalRegistriesRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.AdELegalOKDto;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.AdELegalRequestBodyDto;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.GetAddressRegistroImpreseOKDto;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.LegalInstitutionsRequestBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import java.net.URI;

@FeignClient(name = "${rest-client.national-registries.serviceCode}", url = "${rest-client.national-registries.base-url}", configuration = NationalRegistriesRestClientConfig.class)
public interface NationalRegistriesRestClient {

    @PostMapping(value = "${rest-client.national-registries.getLegalAddress.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    GetAddressRegistroImpreseOKDto getLegalAddress(
            URI endpoint, @RequestHeader("x-api-key") String apiKey,
            @RequestBody LegalAddressRequest legalAddressRequest);

    @PostMapping(value = "${rest-client.national-registries.verifyLegal.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    AdELegalOKDto verifyLegal(
            URI endpoint, @RequestHeader("x-api-key") String apiKey,
            @RequestBody AdELegalRequestBodyDto request);

    @PostMapping(value = "${rest-client.national-registries.getLegalInstitutions.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    Businesses getLegalInstitutions(
            URI endpoint, @RequestHeader("x-api-key") String apiKey,
            @RequestBody LegalInstitutionsRequestBody request);
}
