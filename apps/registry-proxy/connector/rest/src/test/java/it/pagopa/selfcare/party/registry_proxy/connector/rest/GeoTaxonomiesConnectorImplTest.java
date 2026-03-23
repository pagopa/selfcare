package it.pagopa.selfcare.party.registry_proxy.connector.rest;



import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ServiceUnavailableException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.GeoTaxonomiesRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.geotaxonomy.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.geotaxonomy.GeographicTaxonomyResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.selfcare.party.registry_proxy.connector.rest.GeoTaxonomiesConnectorImpl.CODE_S_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class GeoTaxonomiesConnectorImplTest {
    @InjectMocks
    private GeoTaxonomiesConnectorImpl geoTaxonomiesConnectorImpl;

    @Mock
    private GeoTaxonomiesRestClient geoTaxonomiesRestClient;


    @Test
    void testGetExtByDescription() {

        //given
        String description = "Roma";
        GeographicTaxonomiesResponse geographicTaxonomiesResponse = new GeographicTaxonomiesResponse();
        GeographicTaxonomyResponse geographicTaxonomyResponse = new GeographicTaxonomyResponse();
        geographicTaxonomyResponse.setDescription(description);
        geographicTaxonomyResponse.setGeotaxId("058");
        geographicTaxonomyResponse.setEnabled(true);
        geographicTaxonomyResponse.setRegionId("12");
        geographicTaxonomyResponse.setProvinceId("058");
        geographicTaxonomyResponse.setProvinceAbbreviation("RM");
        geographicTaxonomyResponse.setCountry("100");
        geographicTaxonomyResponse.setCountryAbbreviation("IT");
        geographicTaxonomyResponse.setIstatCode("null");
        List<GeographicTaxonomyResponse> geographicTaxonomyResponseList = new ArrayList<>();
        geographicTaxonomyResponseList.add(geographicTaxonomyResponse);
        geographicTaxonomiesResponse.setGeographicTaxonomiesResponse(geographicTaxonomyResponseList);

        List<GeographicTaxonomy> geographicTaxonomiesList = new ArrayList<>();
        GeographicTaxonomy geographicTaxonomies = new GeographicTaxonomy();
        geographicTaxonomies.setDescription(description);
        geographicTaxonomies.setGeotaxId("058");
        geographicTaxonomies.setEnabled(true);
        geographicTaxonomies.setRegionId("12");
        geographicTaxonomies.setProvinceId("058");
        geographicTaxonomies.setProvinceAbbreviation("RM");
        geographicTaxonomies.setCountry("100");
        geographicTaxonomies.setCountryAbbreviation("IT");
        geographicTaxonomies.setIstatCode("null");
        geographicTaxonomiesList.add(geographicTaxonomies);

        when(geoTaxonomiesRestClient.getExtByDescription(anyString(),any(),any())).thenReturn(geographicTaxonomiesResponse);

        //when
        geographicTaxonomiesList = geoTaxonomiesConnectorImpl.getExtByDescription(description, 0, 10);

        //then
        assertNotNull(geographicTaxonomiesList);
        assertNotNull(geographicTaxonomiesList.getClass());
        assertEquals(1, geographicTaxonomiesList.size());
        GeographicTaxonomy geographicTaxonomy = geographicTaxonomiesList.iterator().next();
        assertEquals(geographicTaxonomyResponse.getDescription(), geographicTaxonomy.getDescription());
        assertEquals(geographicTaxonomyResponse.getIstatCode(), geographicTaxonomy.getIstatCode());
        assertEquals(geographicTaxonomyResponse.getProvinceId(), geographicTaxonomy.getProvinceId());
        assertEquals(geographicTaxonomyResponse.getProvinceAbbreviation(), geographicTaxonomy.getProvinceAbbreviation());
        assertEquals(geographicTaxonomyResponse.getRegionId(), geographicTaxonomy.getRegionId());
        assertEquals(geographicTaxonomyResponse.getGeotaxId(), geographicTaxonomy.getGeotaxId());
        assertEquals(geographicTaxonomyResponse.getCountry(), geographicTaxonomy.getCountry());
        assertEquals(geographicTaxonomyResponse.getCountryAbbreviation(), geographicTaxonomy.getCountryAbbreviation());

        verify(geoTaxonomiesRestClient, times(1))
                .getExtByDescription(anyString(),any(),any());
        verifyNoMoreInteractions(geoTaxonomiesRestClient);

    }

    @Test
    void testGetExtByDescription_nullDescription() {

        //given
        String description = null;
        GeographicTaxonomiesResponse geographicTaxonomiesResponse = new GeographicTaxonomiesResponse();
        GeographicTaxonomyResponse geographicTaxonomyResponse = new GeographicTaxonomyResponse();
        geographicTaxonomyResponse.setDescription(description);
        geographicTaxonomyResponse.setGeotaxId("058");
        geographicTaxonomyResponse.setEnabled(true);
        geographicTaxonomyResponse.setRegionId("12");
        geographicTaxonomyResponse.setProvinceId("058");
        geographicTaxonomyResponse.setProvinceAbbreviation("RM");
        geographicTaxonomyResponse.setCountry("100");
        geographicTaxonomyResponse.setCountryAbbreviation("IT");
        geographicTaxonomyResponse.setIstatCode("null");
        List<GeographicTaxonomyResponse> geographicTaxonomyResponseList = new ArrayList<>();
        geographicTaxonomyResponseList.add(geographicTaxonomyResponse);
        geographicTaxonomiesResponse.setGeographicTaxonomiesResponse(geographicTaxonomyResponseList);

        when(geoTaxonomiesRestClient.getExtByDescription(anyString(),any(),any())).thenReturn(geographicTaxonomiesResponse);

        //when
        Executable executable = () -> geoTaxonomiesConnectorImpl.getExtByDescription(description, 0, 10);

        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Description is required", e.getMessage());
        Mockito.verifyNoInteractions(geoTaxonomiesRestClient);

    }
@Test
    void testGetExtCode() {

        //given
        String code = "058";
        GeographicTaxonomyResponse geographicTaxonomyResponse = new GeographicTaxonomyResponse();
        geographicTaxonomyResponse.setDescription("Roma");
        geographicTaxonomyResponse.setGeotaxId(code);
        geographicTaxonomyResponse.setEnabled(true);
        geographicTaxonomyResponse.setRegionId("12");
        geographicTaxonomyResponse.setProvinceId("058");
        geographicTaxonomyResponse.setProvinceAbbreviation("RM");
        geographicTaxonomyResponse.setCountry("100");
        geographicTaxonomyResponse.setCountryAbbreviation("IT");
        geographicTaxonomyResponse.setIstatCode("null");

        GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
        geographicTaxonomy.setDescription("Roma");
        geographicTaxonomy.setGeotaxId(code);
        geographicTaxonomy.setEnabled(true);
        geographicTaxonomy.setRegionId("12");
        geographicTaxonomy.setProvinceId("058");
        geographicTaxonomy.setProvinceAbbreviation("RM");
        geographicTaxonomy.setCountry("100");
        geographicTaxonomy.setCountryAbbreviation("IT");
        geographicTaxonomy.setIstatCode("null");


        when(geoTaxonomiesRestClient.getExtByCode(anyString())).thenReturn(geographicTaxonomyResponse);

        //when
        geographicTaxonomy = geoTaxonomiesConnectorImpl.getExtByCode(code);

        //then
        assertNotNull(geographicTaxonomy);
        assertEquals(geographicTaxonomyResponse.getDescription(), geographicTaxonomy.getDescription());
        assertEquals(geographicTaxonomyResponse.getIstatCode(), geographicTaxonomy.getIstatCode());
        assertEquals(geographicTaxonomyResponse.getProvinceId(), geographicTaxonomy.getProvinceId());
        assertEquals(geographicTaxonomyResponse.getProvinceAbbreviation(), geographicTaxonomy.getProvinceAbbreviation());
        assertEquals(geographicTaxonomyResponse.getRegionId(), geographicTaxonomy.getRegionId());
        assertEquals(geographicTaxonomyResponse.getGeotaxId(), geographicTaxonomy.getGeotaxId());
        assertEquals(geographicTaxonomyResponse.getCountry(), geographicTaxonomy.getCountry());
        assertEquals(geographicTaxonomyResponse.getCountryAbbreviation(), geographicTaxonomy.getCountryAbbreviation());

        verify(geoTaxonomiesRestClient, times(1))
                .getExtByCode(anyString());
        verifyNoMoreInteractions(geoTaxonomiesRestClient);

    }

    @Test
    void testGetExtByCode_nullCode() {

        //given
        String code = null;

        //when
        Executable executable = () -> geoTaxonomiesConnectorImpl.getExtByCode(code);

        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Code is required", e.getMessage());
        Mockito.verifyNoInteractions(geoTaxonomiesRestClient);
    }



    @Test
    void testGetExtByCode_notFound() {

        //given
        String code = "example";
        when(geoTaxonomiesRestClient.getExtByCode(anyString())).thenReturn(null);

        //when
        Executable executable = () -> geoTaxonomiesConnectorImpl.getExtByCode(code);

        //then
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals(String.format(CODE_S_NOT_FOUND,code), e.getMessage());

    }

    @Test
    void fallbackGetExtByDescriptionTest(){
        List<GeographicTaxonomy> list = geoTaxonomiesConnectorImpl.fallbackGetExtByDescription(new ServiceUnavailableException());
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    void fallbackGetExtByCodeTest(){
        Assertions.assertThrows(ResourceNotFoundException.class, () -> geoTaxonomiesConnectorImpl.fallbackGetExtByCode(new ServiceUnavailableException()));
    }


}

