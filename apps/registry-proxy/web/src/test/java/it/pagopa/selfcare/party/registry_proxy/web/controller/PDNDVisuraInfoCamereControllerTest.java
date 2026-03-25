package it.pagopa.selfcare.party.registry_proxy.web.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;

@ContextConfiguration(classes = {PDNDVisuraInfoCamereController.class, WebTestConfig.class})
@WebMvcTest(value = {PDNDVisuraInfoCamereController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class PDNDVisuraInfoCamereControllerTest {
    private static final String BASE_URL = "/visura-infocamere-pdnd";

    @Autowired
    protected MockMvc mvc;
    @MockBean
    private PDNDInfoCamereService pdndInfoCamereService;
    @MockBean
    private PDNDInfoCamereBusinessMapper pdndBusinessMapper;

    /**
     * Method under test: {@link PDNDVisuraInfoCamereController#institutionsPdndByRea(String)}
     */
    @Test
    void testInstitutionsByRea() throws Exception {
        final String rea = "XX-123456";
        PDNDBusiness pdndBusinesses = dummyPDNDBusiness();
        PDNDBusinessResource pdndBusinessResource = dummyPDNDBusinessResource();

        when(pdndInfoCamereService.retrieveInstitutionFromRea(anyString(), anyString())).thenReturn(pdndBusinesses);
        when(pdndBusinessMapper.toResource(pdndBusinesses)).thenReturn(pdndBusinessResource);

        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/institutions")
                        .param("rea", rea)
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

        verify(pdndInfoCamereService, times(1)).retrieveInstitutionFromRea(anyString(), anyString());
        verify(pdndBusinessMapper, times(1)).toResource(pdndBusinesses);
        verifyNoMoreInteractions(pdndInfoCamereService);
        verifyNoMoreInteractions(pdndBusinessMapper);
    }


    @Test
    void testInstitutionsByRea_malformedParameter() throws Exception {
        final String malformedRea = "rea";

        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/institutions")
                        .param("rea", malformedRea)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("Rea parameter is malformed. It should be in form of XX-123456"));

        verifyNoInteractions(pdndInfoCamereService);
        verifyNoInteractions(pdndBusinessMapper);
    }


    /**
     * Method under test: {@link PDNDVisuraInfoCamereController#getInstitution(String)}
     */
    @Test
    void testInstitutionByTaxCode() throws Exception {
        final String taxCode = "taxCode";
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        PDNDBusinessResource pdndBusinessResource = dummyPDNDBusinessResource();

        when(pdndInfoCamereService.retrieveInstitutionDetail(anyString())).thenReturn(pdndBusiness);
        when(pdndBusinessMapper.toResource(pdndBusiness)).thenReturn(pdndBusinessResource);

        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/institutions/{taxCode}", taxCode)
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

        verify(pdndInfoCamereService, times(1)).retrieveInstitutionDetail(anyString());
        verify(pdndBusinessMapper, times(1)).toResource(pdndBusiness);
        verifyNoMoreInteractions(pdndInfoCamereService);
        verifyNoMoreInteractions(pdndBusinessMapper);
    }

    /**
     * Method under test: {@link PDNDVisuraInfoCamereController#getInstitutionDocument(String)}
     */
    @Test
    void testInstitutionDocumentByTaxCode() throws Exception {
        final String taxCode = "taxCode";
        final String document = "document";

        when(pdndInfoCamereService.retrieveInstitutionDocument(anyString())).thenReturn(document.getBytes(StandardCharsets.UTF_8));

        mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/institutions/{taxCode}/document", taxCode)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        verify(pdndInfoCamereService, times(1)).retrieveInstitutionDocument(anyString());
        verifyNoMoreInteractions(pdndInfoCamereService);
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

