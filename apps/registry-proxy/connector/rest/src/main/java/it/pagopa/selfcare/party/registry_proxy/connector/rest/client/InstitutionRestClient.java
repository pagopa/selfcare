package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.InstitutionRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.institution.InstitutionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "${rest-client.selc-institution.serviceCode}", url = "${rest-client.selc-institution.base-url}", configuration = InstitutionRestClientConfig.class)
public interface InstitutionRestClient {

  @GetMapping(value = "${rest-client.selc-institution.getById.path}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  InstitutionResponse getById(@PathVariable("id") String id);
}