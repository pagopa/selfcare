package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.decoder.FeignXmlDecoder;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@FeignClient(name = "${rest-client.pdnd-infocamere.serviceCode}", url = "${rest-client.pdnd-infocamere.base-url}", configuration = FeignXmlDecoder.class)
public interface PDNDInfoCamereRestClient {

    @GetMapping(value = "${rest-client.pdnd-infocamere.getDescription.path}", consumes = APPLICATION_XML_VALUE)
    @ResponseBody
    List<PDNDImpresa> retrieveInstitutionsPdndByDescription(@RequestParam(value = "denominazione") String description, @RequestHeader("Authorization") String accessToken);

    @GetMapping(value = "${rest-client.pdnd-infocamere.getTaxCode.path}", consumes = APPLICATION_XML_VALUE)
    @ResponseBody
    List<PDNDImpresa> retrieveInstitutionPdndByTaxCode(@RequestParam(value = "codiceFiscale") String taxCode, @RequestHeader("Authorization") String accessToken);

}
