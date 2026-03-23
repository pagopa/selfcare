package it.pagopa.selfcare.party.registry_proxy.web.controller;


import it.pagopa.selfcare.party.registry_proxy.connector.model.GeographicTaxonomy;
import it.pagopa.selfcare.party.registry_proxy.core.GeographicTaxonomiesService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {GeographicTaxonomiesController.class, WebTestConfig.class})
@WebMvcTest(value = {GeographicTaxonomiesController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class GeographicTaxonomiesControllerTest {
    private static final String BASE_URL = "/geotaxonomies";

    @Autowired
    protected MockMvc mvc;

    @MockBean
    private GeographicTaxonomiesService geographicTaxonomiesService;


    @Test
    void retrieveGeoTaxonomiesByDescription() throws Exception {

        //given
        String description = "Roma";
        int offset = 0;
        int limit = 10;
        List<GeographicTaxonomy> geographicTaxonomies = new ArrayList<>();
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
        geographicTaxonomies.add(geographicTaxonomy);
        when(geographicTaxonomiesService.retrieveGeoTaxonomiesByDescription(description, offset, limit)).thenReturn(geographicTaxonomies);


        //when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "?description={description}&offset={offset}&limit={limit}", description, offset, limit)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].desc", notNullValue()))
                .andExpect(jsonPath("$.[0].istat_code", notNullValue()))
                .andExpect(jsonPath("$.[0].province_id", notNullValue()))
                .andExpect(jsonPath("$.[0].province_abbreviation", notNullValue()))
                .andExpect(jsonPath("$.[0].region_id", notNullValue()))
                .andExpect(jsonPath("$.[0].code", notNullValue()))
                .andExpect(jsonPath("$.[0].enabled", notNullValue() ))
                .andExpect(jsonPath("$.[0].country", notNullValue()))
                .andExpect(jsonPath("$.[0].country_abbreviation", notNullValue()));

        //then
        verify(geographicTaxonomiesService, times(1))
                .retrieveGeoTaxonomiesByDescription(anyString(), any(), any());
        verifyNoMoreInteractions(geographicTaxonomiesService);

    }

    @Test
    void retrieveGeoTaxonomiesByCode() throws Exception {

        //given
        String code = "058";
        List<GeographicTaxonomy> geographicTaxonomies = new ArrayList<>();
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
        when(geographicTaxonomiesService.retriveGeoTaxonomyByCode(code)).thenReturn(geographicTaxonomy);


        //when
        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{code}", code)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desc", notNullValue()))
                .andExpect(jsonPath("$.istat_code", notNullValue()))
                .andExpect(jsonPath("$.province_id", notNullValue()))
                .andExpect(jsonPath("$.province_abbreviation", notNullValue()))
                .andExpect(jsonPath("$.region_id", notNullValue()))
                .andExpect(jsonPath("$.code", notNullValue()))
                .andExpect(jsonPath("$.enabled", notNullValue() ))
                .andExpect(jsonPath("$.country", notNullValue()))
                .andExpect(jsonPath("$.country_abbreviation", notNullValue()));

        //then
        verify(geographicTaxonomiesService, times(1))
                .retriveGeoTaxonomyByCode(anyString());
        verifyNoMoreInteractions(geographicTaxonomiesService);

    }
}
