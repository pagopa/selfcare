package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.decoder.FeignXmlDecoder;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDVisuraImpresa;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@FeignClient(name = "${rest-client.pdnd-visura-infocamere.serviceCode}", url = "${rest-client.pdnd-visura-infocamere.base-url}", configuration = FeignXmlDecoder.class)
public interface PDNDVisuraInfoCamereRestClient {

    @GetMapping(value = "${rest-client.pdnd-visura-infocamere.getTaxCode.path}", consumes = APPLICATION_XML_VALUE)
    @ResponseBody
    PDNDVisuraImpresa retrieveInstitutionDetail(@RequestParam(value = "codiceFiscale") String taxCode, @RequestHeader("Authorization") String accessToken);

    @GetMapping(value = "${rest-client.pdnd-visura-infocamere.getRea.path}", consumes = APPLICATION_XML_VALUE)
    @ResponseBody
    List<PDNDImpresa> retrieveInstitutionPdndFromRea(@RequestParam(value = "numeroRea") String rea, @RequestParam("siglaProvincia") String siglaProvincia, @RequestHeader("Authorization") String accessToken);
}
