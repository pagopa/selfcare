package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(name = "${rest-client.ipa-open-data.serviceCode}", url = "${rest-client.ipa-open-data.base-url}")
public interface IPAOpenDataRestClient extends OpenDataRestClient {

    @GetMapping(value = "${rest-client.ipa-open-data.retrieveInstitutions.path}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    String retrieveInstitutions();

    @GetMapping(value = "${rest-client.ipa-open-data.retrieveCategories.path}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    String retrieveCategories();

    @GetMapping(value = "${rest-client.ipa-open-data.retrieveAOOs.path}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    String retrieveAOOs();

    @GetMapping(value = "${rest-client.ipa-open-data.retrieveUOs.path}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    String retrieveUOs();

    @GetMapping(value = "${rest-client.ipa-open-data.retrieveUOsWithSfe.path}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    String retrieveUOsWithSfe();
}
