package it.pagopa.selfcare.party.registry_proxy.web.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import it.pagopa.selfcare.party.registry_proxy.core.PDNDInfoCamereService;
import it.pagopa.selfcare.party.registry_proxy.web.config.WebTestConfig;
import it.pagopa.selfcare.party.registry_proxy.web.model.PDNDBusinessResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.PDNDInfoCamereBusinessMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

@ContextConfiguration(classes = {PDNDInfoCamereController.class, WebTestConfig.class})
@WebMvcTest(value = {PDNDInfoCamereController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class PDNDInfoCamereControllerTest {
    private static final String BASE_URL = "/infocamere-pdnd";

    @Autowired
    protected MockMvc mvc;
    @MockBean
    private PDNDInfoCamereService pdndInfoCamereService;
    @MockBean
    private PDNDInfoCamereBusinessMapper pdndBusinessMapper;



    @Test
    void testInstitutionsByDescription() throws Exception {
        String description = "description";
        List<PDNDBusiness> pdndBusinesses = new ArrayList<>();
        pdndBusinesses.add(dummyPDNDBusiness());

        List<PDNDBusinessResource> pdndBusinessResources = new ArrayList<>();
        pdndBusinessResources.add(dummyPDNDBusinessResource());

        when(pdndInfoCamereService.retrieveInstitutionsPdndByDescription(anyString())).thenReturn(pdndBusinesses);
        when(pdndBusinessMapper.toResources(pdndBusinesses)).thenReturn(pdndBusinessResources);

        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/institutions")
                        .param("description", description)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].businessTaxId", is("taxId")))
                .andExpect(jsonPath("$[0].businessName", is("description")))
                .andExpect(jsonPath("$[0].legalNature", is("legalNature")))
                .andExpect(jsonPath("$[0].legalNatureDescription", is("legalNatureDescription")))
                .andExpect(jsonPath("$[0].cciaa", is("cciaa")))
                .andExpect(jsonPath("$[0].nRea", is("nRea")))
                .andExpect(jsonPath("$[0].businessStatus", is("status")))
                .andExpect(jsonPath("$[0].city", is("city")))
                .andExpect(jsonPath("$[0].county", is("county")))
                .andExpect(jsonPath("$[0].zipCode", is("zipCode")))
                .andExpect(jsonPath("$[0].address", is("address")))
                .andExpect(jsonPath("$[0].digitalAddress", is("digitalAddress")))
                .andReturn();

        verify(pdndInfoCamereService, times(1)).retrieveInstitutionsPdndByDescription(anyString());
        verify(pdndBusinessMapper, times(1)).toResources(pdndBusinesses);
        verifyNoMoreInteractions(pdndInfoCamereService);
        verifyNoMoreInteractions(pdndBusinessMapper);
    }

    @Test
    void testInstitutionByTaxCode() throws Exception {
        String taxCode = "taxCode";
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        PDNDBusinessResource pdndBusinessResource = dummyPDNDBusinessResource();

        when(pdndInfoCamereService.retrieveInstitutionPdndByTaxCode(anyString())).thenReturn(pdndBusiness);
        when(pdndBusinessMapper.toResource(pdndBusiness)).thenReturn(pdndBusinessResource);

        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/institution/{taxCode}", taxCode)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessTaxId", is("taxId")))
                .andExpect(jsonPath("$.businessName", is("description")))
                .andExpect(jsonPath("$.legalNature", is("legalNature")))
                .andExpect(jsonPath("$.legalNatureDescription", is("legalNatureDescription")))
                .andExpect(jsonPath("$.cciaa", is("cciaa")))
                .andExpect(jsonPath("$.nRea", is("nRea")))
                .andExpect(jsonPath("$.businessStatus", is("status")))
                .andExpect(jsonPath("$.city", is("city")))
                .andExpect(jsonPath("$.county", is("county")))
                .andExpect(jsonPath("$.zipCode", is("zipCode")))
                .andExpect(jsonPath("$.address", is("address")))
                .andExpect(jsonPath("$.digitalAddress", is("digitalAddress")))
                .andReturn();

        verify(pdndInfoCamereService, times(1)).retrieveInstitutionPdndByTaxCode(anyString());
        verify(pdndBusinessMapper, times(1)).toResource(pdndBusiness);
        verifyNoMoreInteractions(pdndInfoCamereService);
        verifyNoMoreInteractions(pdndBusinessMapper);
    }

    private PDNDBusiness dummyPDNDBusiness(){
        PDNDBusiness pdndBusiness = new PDNDBusiness();
        pdndBusiness.setBusinessTaxId("taxId");
        pdndBusiness.setBusinessName("description");
        pdndBusiness.setBusinessStatus("status");
        pdndBusiness.setCity("city");
        pdndBusiness.setCciaa("cciaa");
        pdndBusiness.setAddress("address");
        pdndBusiness.setDigitalAddress("digitalAddress");
        pdndBusiness.setCounty("county");
        pdndBusiness.setLegalNature("legalNature");
        pdndBusiness.setLegalNatureDescription("legalNatureDescription");
        pdndBusiness.setNRea("nRea");
        pdndBusiness.setZipCode("zipCode");
        return pdndBusiness;
    }

    private PDNDBusinessResource dummyPDNDBusinessResource(){
        PDNDBusinessResource pdndBusinessResource = new PDNDBusinessResource();
        pdndBusinessResource.setBusinessTaxId("taxId");
        pdndBusinessResource.setBusinessName("description");
        pdndBusinessResource.setLegalNature("legalNature");
        pdndBusinessResource.setLegalNatureDescription("legalNatureDescription");
        pdndBusinessResource.setCciaa("cciaa");
        pdndBusinessResource.setNRea("nRea");
        pdndBusinessResource.setBusinessStatus("status");
        pdndBusinessResource.setCity("city");
        pdndBusinessResource.setCounty("county");
        pdndBusinessResource.setZipCode("zipCode");
        pdndBusinessResource.setAddress("address");
        pdndBusinessResource.setDigitalAddress("digitalAddress");
        return pdndBusinessResource;
    }
}

