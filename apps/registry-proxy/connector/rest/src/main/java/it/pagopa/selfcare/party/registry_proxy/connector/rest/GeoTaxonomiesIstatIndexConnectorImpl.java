package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.api.GeoTaxonomiesIstatIndexConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.GeoTaxonomyIstatIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GeoTaxonomiesIstatIndexConnectorImpl implements GeoTaxonomiesIstatIndexConnector {

    /**
     * Whitelist applied before building the OData $filter, to prevent filter injection.
     */
    static final Pattern VALID_CODE_PATTERN = Pattern.compile("^[0-9A-Za-z]{1,20}$");

    private final AzureSearchRestClient azureSearchRestClient;

    public GeoTaxonomiesIstatIndexConnectorImpl(AzureSearchRestClient azureSearchRestClient) {
        this.azureSearchRestClient = azureSearchRestClient;
    }

    @Override
    public Optional<GeographicTaxonomy> findByCode(String code) {
        final String safeCode = Objects.isNull(code) ? null : Encode.forJava(code);
        if (Objects.isNull(code) || !VALID_CODE_PATTERN.matcher(code).matches()) {
            log.warn("Invalid geotaxonomy code format for fallback ISTAT index lookup: {}",
                    safeCode);
            return Optional.empty();
        }

        String filter = "id eq '" + code + "'";
        try {
            SearchServiceIndexResponse<GeoTaxonomyIstatIndex> response =
                    azureSearchRestClient.searchGeoTaxonomyIstat(filter, 1);

            if (Objects.isNull(response) || Objects.isNull(response.getValue()) || response.getValue().isEmpty()) {
                log.debug("Geotaxonomy code {} not present on fallback ISTAT index", safeCode);
                return Optional.empty();
            }
            return Optional.of(toGeographicTaxonomy(response.getValue().get(0)));
        } catch (Exception e) {
            log.error("Error querying fallback ISTAT index for geotaxonomy code {}: {}", safeCode, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private GeographicTaxonomy toGeographicTaxonomy(GeoTaxonomyIstatIndex document) {
        GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
        geographicTaxonomy.setGeotaxId(Objects.nonNull(document.getGeotaxId()) ? document.getGeotaxId() : document.getId());
        geographicTaxonomy.setIstatCode(Objects.nonNull(document.getIstatCode()) ? document.getIstatCode() : document.getId());
        geographicTaxonomy.setDescription(document.getDescription());
        geographicTaxonomy.setProvinceId(document.getProvinceId());
        geographicTaxonomy.setProvinceAbbreviation(document.getProvinceAbbreviation());
        geographicTaxonomy.setRegionId(document.getRegionId());
        geographicTaxonomy.setCountry(document.getCountry());
        geographicTaxonomy.setCountryAbbreviation(document.getCountryAbbreviation());
        geographicTaxonomy.setEnabled(!Boolean.FALSE.equals(document.getEnabled()));
        return geographicTaxonomy;
    }
}


