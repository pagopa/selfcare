package it.pagopa.selfcare.mscore.connector.rest.client;

import it.pagopa.selfcare.mscore.connector.rest.config.PartyRegistryProxyRestClientConfig;
import it.pagopa.selfcare.mscore.connector.rest.model.geotaxonomy.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.mscore.connector.rest.model.registryproxy.*;
import it.pagopa.selfcare.mscore.model.institution.NationalRegistriesProfessionalAddress;
import it.pagopa.selfcare.registry_proxy.generated.openapi.v1.api.InsuranceCompaniesApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "${rest-client.party-registry-proxy.serviceCode}", url = "${rest-client.party-registry-proxy.base-url}", configuration = PartyRegistryProxyRestClientConfig.class)
public interface PartyRegistryProxyRestClient extends InsuranceCompaniesApi {

    @GetMapping(value = "${rest-client.party-registry-proxy.getInstitutionById.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    ProxyInstitutionResponse getInstitutionById(@PathVariable("institutionId") String id);

    @GetMapping(value = "${rest-client.party-registry-proxy.getCategory.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    ProxyCategoryResponse getCategory(@PathVariable("origin") String origin, @PathVariable("code") String code);

    @PostMapping(value = "${rest-client.party-registry-proxy.getInstitutionsByLegal.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    InstitutionsByLegalResponse getInstitutionsByLegal(@RequestBody InstitutionsByLegalRequest institutions);

    @GetMapping(value = "${rest-client.party-registry-proxy.getLegalAddress.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    NationalRegistriesProfessionalAddress getLegalAddress(@RequestParam(value = "taxId") String taxId);

    @GetMapping(value = "${rest-client.party-registry-proxy.geo-taxonomies.getByCode.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    GeographicTaxonomiesResponse getExtByCode(@PathVariable(value = "geotax_id") String code);

    @GetMapping(value = "${rest-client.party-registry-proxy.aoo.getByCode.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    AooResponse getAooById(@PathVariable(value = "aooId") String aooId);

    @GetMapping(value = "${rest-client.party-registry-proxy.uo.getByCode.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    UoResponse getUoById(@PathVariable(value = "uoId") String uoId);

    @GetMapping(value = "${rest-client.party-registry-proxy.sa.getByTaxId.path}", consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    PdndResponse getSaByTaxId(@PathVariable(value = "taxId") String taxId);

}
