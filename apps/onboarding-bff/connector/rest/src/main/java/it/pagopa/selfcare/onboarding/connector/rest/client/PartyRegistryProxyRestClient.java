package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.connector.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.connector.model.institutions.MatchInfoResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.connector.rest.model.AooResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.UoResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.institution_pnpg.InstitutionByLegalTaxIdRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Party Registry Proxy Rest Client
 */
@FeignClient(name = "${rest-client.party-registry-proxy.serviceCode}", url = "${rest-client.party-registry-proxy.base-url}")
public interface PartyRegistryProxyRestClient {

    @PostMapping(value = "${rest-client.party-registry-proxy.getInstitutionsByUserLegalTaxId.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionInfoIC getInstitutionsByUserLegalTaxId(@RequestBody InstitutionByLegalTaxIdRequest request);

    @GetMapping(value = "${rest-client.party-registry-proxy.matchInstitutionAndUser.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    MatchInfoResult matchInstitutionAndUser(@RequestParam(value = "vatNumber") String institutionExternalId,
                                            @RequestParam(value = "taxId") String taxCode);

    @GetMapping(value = "${rest-client.party-registry-proxy.getInstitutionLegalAddress.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionLegalAddressData getInstitutionLegalAddress(@RequestParam(value = "taxId") String externalInstitutionId);

    @GetMapping(value = "${rest-client.party-registry-proxy.getInstitutionById.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    ProxyInstitutionResponse getInstitutionById(@PathVariable("institutionId") String id);

    @GetMapping(value = "${rest-client.party-registry-proxy.geo-taxonomies.getByCode.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    GeographicTaxonomiesResponse getExtByCode(@PathVariable(value = "geotax_id") String code);

    @GetMapping(value = "${rest-client.party-registry-proxy.aoo.getByCode.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    AooResponse getAooById(@PathVariable(value = "aooId") String aooId);

    @GetMapping(value = "${rest-client.party-registry-proxy.uo.getByCode.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    UoResponse getUoById(@PathVariable(value = "uoId") String uoId);
}
