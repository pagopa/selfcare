package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.encoder.FormFeignEncoderConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDTokenForm;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "${rest-client.pdnd.serviceCode}", url = "${rest-client.pdnd.base-url}", configuration = FormFeignEncoderConfig.class)
public interface PdndRestClient {
    @PostMapping(value = "/token.oauth2", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ClientCredentialsResponse createToken(PDNDTokenForm form);
}