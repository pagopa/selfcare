package it.pagopa.selfcare.party.registry_proxy.core;


import it.pagopa.selfcare.party.registry_proxy.connector.api.GeoTaxonomiesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {GeographicTaxonomiesServiceImpl.class})
public class GeographicTaxonomiesServiceImplTest {

    @Autowired
    private GeographicTaxonomiesService geographicTaxonomiesService;
    @MockBean
    private GeoTaxonomiesConnector geoTaxonomiesConnector;


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
