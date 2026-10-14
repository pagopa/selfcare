package it.pagopa.selfcare.party.registry_proxy.core;


import it.pagopa.selfcare.party.registry_proxy.connector.api.GeoTaxonomiesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.GeoTaxonomiesIstatIndexConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.BadGatewayException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {GeographicTaxonomiesServiceImpl.class})
@TestPropertySource(properties = "geotax.istat-fallback.enabled=true")
public class GeographicTaxonomiesServiceImplTest {

    @Autowired
    private GeographicTaxonomiesService geographicTaxonomiesService;
    @MockBean
    private GeoTaxonomiesConnector geoTaxonomiesConnector;
    @MockBean
    private GeoTaxonomiesIstatIndexConnector geoTaxonomiesIstatIndexConnector;


    /**
     * Method under test: {@link GeographicTaxonomiesServiceImpl#retrieveGeoTaxonomiesByDescription(String,Integer,Integer)}
     */

    @Test
    void retrieveGeoTaxonomiesByDescription() {

        //given
        String description = "Roma";
        List<GeographicTaxonomy> geographicTaxonomiesList = new ArrayList<>();
        GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
        geographicTaxonomy.setDescription(description);
        geographicTaxonomy.setGeotaxId("058");
        geographicTaxonomy.setEnabled(true);
        geographicTaxonomy.setRegionId("12");
        geographicTaxonomy.setProvinceId("058");
        geographicTaxonomy.setProvinceAbbreviation("RM");
        geographicTaxonomy.setCountry("100");
        geographicTaxonomy.setCountryAbbreviation("IT");
        geographicTaxonomy.setIstatCode("null");
        geographicTaxonomiesList.add(geographicTaxonomy);
        when(geoTaxonomiesConnector.getExtByDescription(anyString(),any(),any())).thenReturn(geographicTaxonomiesList);

        //when
        geographicTaxonomiesList = geographicTaxonomiesService.retrieveGeoTaxonomiesByDescription(description, 0, 10);


        //then
        assertNotNull(geographicTaxonomiesList);
        assertNotNull(geographicTaxonomiesList.getClass());
        assertEquals(1, geographicTaxonomiesList.size());
        verify(geoTaxonomiesConnector, times(1))
                .getExtByDescription(any(),any(),any());
        verifyNoMoreInteractions(geoTaxonomiesConnector);
    }

    @Test
    void retrieveGeoTaxonomiesByDescription_nullDescription() {

        //given
        String description = null;
        List<GeographicTaxonomy> geographicTaxonomiesList = new ArrayList<>();
        GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
        geographicTaxonomy.setDescription(description);
        geographicTaxonomy.setGeotaxId("058");
        geographicTaxonomy.setEnabled(true);
        geographicTaxonomy.setRegionId("12");
        geographicTaxonomy.setProvinceId("058");
        geographicTaxonomy.setProvinceAbbreviation("RM");
        geographicTaxonomy.setCountry("100");
        geographicTaxonomy.setCountryAbbreviation("IT");
        geographicTaxonomy.setIstatCode("null");
        geographicTaxonomiesList.add(geographicTaxonomy);
        when(geoTaxonomiesConnector.getExtByDescription(any(), any(), any())).thenReturn(geographicTaxonomiesList);

        //when
        Executable executable = () -> geographicTaxonomiesService.retrieveGeoTaxonomiesByDescription(description, 0, 10);

        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Description is required", e.getMessage());
        Mockito.verifyNoInteractions(geoTaxonomiesConnector);
    }

    @Test
    void retrieveGeoTaxonomiesByCode() {

        //given
        String code = "058";
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
        when(geoTaxonomiesConnector.getExtByCode(anyString())).thenReturn(geographicTaxonomy);

        //when
        geographicTaxonomy = geographicTaxonomiesService.retriveGeoTaxonomyByCode(code);


        //then
        assertNotNull(geographicTaxonomy);
        verify(geoTaxonomiesConnector, times(1))
                .getExtByCode(code);
        verifyNoMoreInteractions(geoTaxonomiesConnector);
        verifyNoInteractions(geoTaxonomiesIstatIndexConnector);
    }

    @Test
    void retrieveGeoTaxonomiesByCode_datalakeNotFound_foundOnIstatIndex() {
        //given
        String code = "111001";
        GeographicTaxonomy fromIndex = buildGeographicTaxonomy(code);
        when(geoTaxonomiesConnector.getExtByCode(code)).thenThrow(new ResourceNotFoundException("Code 111001 not found!"));
        when(geoTaxonomiesIstatIndexConnector.findByCode(code)).thenReturn(Optional.of(fromIndex));

        //when
        GeographicTaxonomy result = geographicTaxonomiesService.retriveGeoTaxonomyByCode(code);

        //then
        assertSame(fromIndex, result);
        verify(geoTaxonomiesConnector, times(1)).getExtByCode(code);
        verify(geoTaxonomiesIstatIndexConnector, times(1)).findByCode(code);
    }

    @Test
    void retrieveGeoTaxonomiesByCode_datalakeNotFound_notFoundOnIstatIndex() {
        //given
        String code = "111001";
        ResourceNotFoundException datalakeError = new ResourceNotFoundException("Code 111001 not found!");
        when(geoTaxonomiesConnector.getExtByCode(code)).thenThrow(datalakeError);
        when(geoTaxonomiesIstatIndexConnector.findByCode(code)).thenReturn(Optional.empty());

        //when
        Executable executable = () -> geographicTaxonomiesService.retriveGeoTaxonomyByCode(code);

        //then
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertSame(datalakeError, e);
        verify(geoTaxonomiesIstatIndexConnector, times(1)).findByCode(code);
    }

    @Test
    void retrieveGeoTaxonomiesByCode_datalakeUnavailable_foundOnIstatIndex() {
        //given
        String code = "111001";
        GeographicTaxonomy fromIndex = buildGeographicTaxonomy(code);
        when(geoTaxonomiesConnector.getExtByCode(code)).thenThrow(new BadGatewayException("Geo-taxonomies service unavailable"));
        when(geoTaxonomiesIstatIndexConnector.findByCode(code)).thenReturn(Optional.of(fromIndex));

        //when
        GeographicTaxonomy result = geographicTaxonomiesService.retriveGeoTaxonomyByCode(code);

        //then
        assertSame(fromIndex, result);
        verify(geoTaxonomiesIstatIndexConnector, times(1)).findByCode(code);
    }

    @Test
    void retrieveGeoTaxonomiesByCode_datalakeUnavailable_notFoundOnIstatIndex() {
        //given
        String code = "111001";
        BadGatewayException datalakeError = new BadGatewayException("Geo-taxonomies service unavailable");
        when(geoTaxonomiesConnector.getExtByCode(code)).thenThrow(datalakeError);
        when(geoTaxonomiesIstatIndexConnector.findByCode(code)).thenReturn(Optional.empty());

        //when
        Executable executable = () -> geographicTaxonomiesService.retriveGeoTaxonomyByCode(code);

        //then
        BadGatewayException e = assertThrows(BadGatewayException.class, executable);
        assertSame(datalakeError, e);
        verify(geoTaxonomiesIstatIndexConnector, times(1)).findByCode(code);
    }

    @Test
    void retrieveGeoTaxonomiesByCode_unexpectedError_noFallback() {
        //given
        String code = "111001";
        when(geoTaxonomiesConnector.getExtByCode(code)).thenThrow(new IllegalStateException("unexpected"));

        //when
        Executable executable = () -> geographicTaxonomiesService.retriveGeoTaxonomyByCode(code);

        //then
        assertThrows(IllegalStateException.class, executable);
        verifyNoInteractions(geoTaxonomiesIstatIndexConnector);
    }

    @Test
    void retrieveGeoTaxonomiesByCode_fallbackDisabled_notFound() {
        //given
        String code = "111001";
        GeographicTaxonomiesServiceImpl serviceWithFallbackDisabled =
                new GeographicTaxonomiesServiceImpl(geoTaxonomiesConnector, geoTaxonomiesIstatIndexConnector, false);
        ResourceNotFoundException datalakeError = new ResourceNotFoundException("Code 111001 not found!");
        when(geoTaxonomiesConnector.getExtByCode(code)).thenThrow(datalakeError);

        //when
        Executable executable = () -> serviceWithFallbackDisabled.retriveGeoTaxonomyByCode(code);

        //then
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertSame(datalakeError, e);
        verifyNoInteractions(geoTaxonomiesIstatIndexConnector);
    }

    @Test
    void retrieveGeoTaxonomiesByCode_fallbackDisabled_unavailable() {
        //given
        String code = "111001";
        GeographicTaxonomiesServiceImpl serviceWithFallbackDisabled =
                new GeographicTaxonomiesServiceImpl(geoTaxonomiesConnector, geoTaxonomiesIstatIndexConnector, false);
        when(geoTaxonomiesConnector.getExtByCode(code)).thenThrow(new BadGatewayException("Geo-taxonomies service unavailable"));

        //when
        Executable executable = () -> serviceWithFallbackDisabled.retriveGeoTaxonomyByCode(code);

        //then
        assertThrows(BadGatewayException.class, executable);
        verifyNoInteractions(geoTaxonomiesIstatIndexConnector);
    }

    private static GeographicTaxonomy buildGeographicTaxonomy(String code) {
        GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
        geographicTaxonomy.setGeotaxId(code);
        geographicTaxonomy.setIstatCode(code);
        geographicTaxonomy.setDescription("CAGLIARI - COMUNE");
        geographicTaxonomy.setProvinceId("111");
        geographicTaxonomy.setProvinceAbbreviation("CA");
        geographicTaxonomy.setRegionId("20");
        geographicTaxonomy.setCountry("100");
        geographicTaxonomy.setCountryAbbreviation("IT");
        geographicTaxonomy.setEnabled(true);
        return geographicTaxonomy;
    }

    @Test
    void retrieveGeoTaxonomiesByCode_nullCode() {

        //given
        String code = null;

        //when
        Executable executable = () -> geographicTaxonomiesService.retriveGeoTaxonomyByCode(code);

        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Code is required", e.getMessage());
        Mockito.verifyNoInteractions(geoTaxonomiesConnector);
    }



}
