package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.GeoTaxonomiesRestClientConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.geotaxonomy.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.geotaxonomy.GeographicTaxonomyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "${rest-client.geo-taxonomies.serviceCode}", url = "${rest-client.geo-taxonomies.base-url}", configuration = GeoTaxonomiesRestClientConfig.class)
public interface GeoTaxonomiesRestClient {

    @GetMapping(value = "${rest-client.geo-taxonomies.getByDescription.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    GeographicTaxonomiesResponse getExtByDescription(@RequestParam(value = "description") String description,
                                                     @RequestParam(value = "offset") Integer offset,
                                                     @RequestParam(value = "limit") Integer limit);

    @GetMapping(value = "${rest-client.geo-taxonomies.getByCode.path}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    GeographicTaxonomyResponse getExtByCode(@PathVariable("geotax_id") String code);

}
