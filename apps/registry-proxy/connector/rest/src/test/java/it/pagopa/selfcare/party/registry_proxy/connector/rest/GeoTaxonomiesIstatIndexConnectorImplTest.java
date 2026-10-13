package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.exception.ServiceUnavailableException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.AzureSearchRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.GeoTaxonomyIstatIndex;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.search.SearchServiceIndexResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoTaxonomiesIstatIndexConnectorImplTest {

    @InjectMocks
    private GeoTaxonomiesIstatIndexConnectorImpl connector;

    @Mock
    private AzureSearchRestClient azureSearchRestClient;

    @Test
    void findByCode_found_mapsAllFields() {
        GeoTaxonomyIstatIndex document = buildDocument("111001");
        when(azureSearchRestClient.searchGeoTaxonomyIstat(anyString(), anyInt())).thenReturn(buildResponse(List.of(document)));

        Optional<GeographicTaxonomy> result = connector.findByCode("111001");

        assertTrue(result.isPresent());
        GeographicTaxonomy geo = result.get();
        assertEquals("111001", geo.getGeotaxId());
        assertEquals("111001", geo.getIstatCode());
        assertEquals("CAGLIARI - COMUNE", geo.getDescription());
        assertEquals("111", geo.getProvinceId());
        assertEquals("CA", geo.getProvinceAbbreviation());
        assertEquals("20", geo.getRegionId());
        assertEquals("100", geo.getCountry());
        assertEquals("IT", geo.getCountryAbbreviation());
        assertTrue(geo.isEnabled());

        ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> topCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(azureSearchRestClient).searchGeoTaxonomyIstat(filterCaptor.capture(), topCaptor.capture());
        assertEquals("id eq '111001'", filterCaptor.getValue());
        assertEquals(1, topCaptor.getValue());
    }

    @Test
    void findByCode_missingGeotaxIdAndIstatCode_fallbackOnId() {
        GeoTaxonomyIstatIndex document = buildDocument("111001");
        document.setGeotaxId(null);
        document.setIstatCode(null);
        document.setEnabled(null);
        when(azureSearchRestClient.searchGeoTaxonomyIstat(anyString(), anyInt())).thenReturn(buildResponse(List.of(document)));

        Optional<GeographicTaxonomy> result = connector.findByCode("111001");

        assertTrue(result.isPresent());
        assertEquals("111001", result.get().getGeotaxId());
        assertEquals("111001", result.get().getIstatCode());
        assertTrue(result.get().isEnabled());
    }

    @Test
    void findByCode_disabledDocument_mapsEnabledFalse() {
        GeoTaxonomyIstatIndex document = buildDocument("111001");
        document.setEnabled(false);
        when(azureSearchRestClient.searchGeoTaxonomyIstat(anyString(), anyInt())).thenReturn(buildResponse(List.of(document)));

        Optional<GeographicTaxonomy> result = connector.findByCode("111001");

        assertTrue(result.isPresent());
        assertFalse(result.get().isEnabled());
    }

    @Test
    void findByCode_emptyValue_returnsEmpty() {
        when(azureSearchRestClient.searchGeoTaxonomyIstat(anyString(), anyInt())).thenReturn(buildResponse(Collections.emptyList()));

        assertTrue(connector.findByCode("111001").isEmpty());
    }

    @Test
    void findByCode_nullValue_returnsEmpty() {
        when(azureSearchRestClient.searchGeoTaxonomyIstat(anyString(), anyInt())).thenReturn(buildResponse(null));

        assertTrue(connector.findByCode("111001").isEmpty());
    }

    @Test
    void findByCode_nullResponse_returnsEmpty() {
        when(azureSearchRestClient.searchGeoTaxonomyIstat(anyString(), anyInt())).thenReturn(null);

        assertTrue(connector.findByCode("111001").isEmpty());
    }

    @Test
    void findByCode_clientError_returnsEmpty() {
        when(azureSearchRestClient.searchGeoTaxonomyIstat(anyString(), anyInt())).thenThrow(new ServiceUnavailableException());

        assertTrue(connector.findByCode("111001").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"111001' or id ne '", "11 1001", "111001\n", "abc$", "123456789012345678901"})
    void findByCode_invalidCode_noCall(String code) {
        assertTrue(connector.findByCode(code).isEmpty());
        verifyNoInteractions(azureSearchRestClient);
    }

    private static SearchServiceIndexResponse<GeoTaxonomyIstatIndex> buildResponse(List<GeoTaxonomyIstatIndex> value) {
        SearchServiceIndexResponse<GeoTaxonomyIstatIndex> response = new SearchServiceIndexResponse<>();
        response.setValue(value);
        return response;
    }

    private static GeoTaxonomyIstatIndex buildDocument(String code) {
        GeoTaxonomyIstatIndex document = new GeoTaxonomyIstatIndex();
        document.setId(code);
        document.setGeotaxId(code);
        document.setIstatCode(code);
        document.setDescription("CAGLIARI - COMUNE");
        document.setProvinceId("111");
        document.setProvinceAbbreviation("CA");
        document.setRegionId("20");
        document.setCountry("100");
        document.setCountryAbbreviation("IT");
        document.setEnabled(true);
        document.setUpdateDate("2026-10-13");
        return document;
    }
}

