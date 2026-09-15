package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.PDNDInfoCamereRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.PdndSecretValueResolver;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ClientCredentialsResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PDNDSedeImpresa;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.mapper.PDNDBusinessMapper;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.service.PDNDCacheableService;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.service.TokenProviderPDND;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class PDNDInfoCamereConnectorImplTest {

    @InjectMocks
    private PDNDInfoCamereConnectorImpl pdndInfoCamereConnector;
    @Mock
    private PDNDInfoCamereRestClient pdndInfoCamereRestClient;
    @Mock
    private TokenProviderPDND tokenProviderPDND;
    @Mock
    private PDNDBusinessMapper pdndBusinessMapper;
    @Mock
    private PdndSecretValueResolver pdndSecretValueResolver;
    @Mock
    private PDNDCacheableService pdndCacheableService;

    private MockedStatic<DataEncryptionUtils> dataEncryptionUtilsMock;

    @BeforeEach
    void initStaticMocks() {
        dataEncryptionUtilsMock = mockStatic(DataEncryptionUtils.class);
    }

    @AfterEach
    void closeStaticMocks() {
        if (dataEncryptionUtilsMock != null) {
            dataEncryptionUtilsMock.close();
            dataEncryptionUtilsMock = null;
        }
    }

    @Test
    void testRetrieveInstitutionsByDescription() {

        // given
        String description = "description";
        List<PDNDBusiness> pdndBusinesses = new ArrayList<>();
        pdndBusinesses.add(dummyPDNDBusiness());
        List<PDNDImpresa> pdndImpresaList = new ArrayList<>();
        pdndImpresaList.add(dummyPDNDImpresa());

        mockPdndSecretValue();
        mockPdndToken();
        when(pdndInfoCamereRestClient.retrieveInstitutionsPdndByDescription(anyString(), anyString()))
                .thenReturn(pdndImpresaList);
        when(pdndBusinessMapper.toPDNDBusinesses(pdndImpresaList)).thenReturn(pdndBusinesses);

        // when
        pdndBusinesses = pdndInfoCamereConnector.retrieveInstitutionsPdndByDescription(description, "prod-test");

        // then
        assertNotNull(pdndBusinesses);
        assertEquals(1, pdndBusinesses.size());
        PDNDBusiness pdndBusiness = pdndBusinesses.iterator().next();
        assertEquals(dummyPDNDImpresa().getBusinessTaxId(), pdndBusiness.getBusinessTaxId());
        assertEquals(dummyPDNDImpresa().getBusinessName(), pdndBusiness.getBusinessName());

        verify(pdndInfoCamereRestClient, times(1))
                .retrieveInstitutionsPdndByDescription(anyString(), anyString());
        verifyNoMoreInteractions(pdndInfoCamereRestClient);
    }

    @Test
    void testRetrieveInstitutionsByRea() {

        // given
        final String rea = "rea";
        final String county = "county";
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        PDNDImpresa pdndImpresa = dummyPDNDImpresa();

        mockPdndSecretValue();
        mockPdndToken();
        when(pdndInfoCamereRestClient.retrieveInstitutionPdndFromRea(anyString(), anyString(), anyString()))
                .thenReturn(List.of(pdndImpresa));
        when(pdndBusinessMapper.toPDNDBusiness(pdndImpresa)).thenReturn(pdndBusiness);

        // when
        PDNDBusiness result = pdndInfoCamereConnector.retrieveInstitutionFromRea(county, rea, "prod-test");

        // then
        assertNotNull(result);
        assertEquals(dummyPDNDImpresa().getBusinessTaxId(), result.getBusinessTaxId());
        assertEquals(dummyPDNDImpresa().getBusinessName(), result.getBusinessName());

        verify(pdndInfoCamereRestClient, times(1))
                .retrieveInstitutionPdndFromRea(anyString(), anyString(), anyString());
        verifyNoMoreInteractions(pdndInfoCamereRestClient);
    }

    @Test
    void testRetrieveInstitutionsByDescription_nullDescription() {

        // given
        String description = null;

        // when
        Executable executable =
                () -> pdndInfoCamereConnector.retrieveInstitutionsPdndByDescription(description, null);

        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Description is required", e.getMessage());
        Mockito.verifyNoInteractions(pdndInfoCamereRestClient);
    }

    @Test
    void testRetrieveInstitutionsByRea_nullRea() {

        // given
        final String rea = null;

        // when
        Executable executable =
                () -> pdndInfoCamereConnector.retrieveInstitutionFromRea("county", rea, null);

        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Rea is required", e.getMessage());
        Mockito.verifyNoInteractions(pdndInfoCamereRestClient);
    }

    @Test
    void testRetrieveInstitutionsByRea_notFound() {

        // given
        final String rea = "test";
        final String county = "county";
        mockPdndSecretValue();
        mockPdndToken();
        when(pdndInfoCamereRestClient.retrieveInstitutionPdndFromRea(
                anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        // when
        Executable executable =
                () -> pdndInfoCamereConnector.retrieveInstitutionFromRea(county, rea, "prod-test");

        // then
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals("No institution found with rea: " + county + "-" + rea, e.getMessage());
    }

    @Test
    void testRetrieveInstitutionsByRea_notFoundAndNull() {

        // given
        final String rea = "test";
        final String county = "county";
        mockPdndSecretValue();
        mockPdndToken();
        when(pdndInfoCamereRestClient.retrieveInstitutionPdndFromRea(
                anyString(), anyString(), anyString()))
                .thenReturn(null);

        // when
        Executable executable =
                () -> pdndInfoCamereConnector.retrieveInstitutionFromRea(county, rea, "prod-test");

        // then
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals("No institution found with rea: " + county + "-" + rea, e.getMessage());
    }

    @Test
    void retrieveinstitutionbytaxCodeTest() throws JsonProcessingException {

        // given
        String taxCode = "taxCode";
        PDNDImpresa pdndImpresa = dummyPDNDImpresa();
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();

        String encTaxCode = "TEST-STRING";

        when(DataEncryptionUtils.encrypt(any())).thenReturn(encTaxCode);
        when(pdndCacheableService.getEncryptedPDNDImpresa(any(), any())).thenReturn(encTaxCode);

        String decResult = new ObjectMapper().writeValueAsString(pdndImpresa);
        when(DataEncryptionUtils.decrypt(any())).thenReturn(decResult);
        when(pdndBusinessMapper.toPDNDBusiness(dummyPDNDImpresa())).thenReturn(pdndBusiness);

        // when
        pdndBusiness = pdndInfoCamereConnector.retrieveInstitutionPdndByTaxCode(taxCode, "prod-test");

        // then
        assertNotNull(pdndBusiness);
        assertEquals(dummyPDNDImpresa().getBusinessTaxId(), pdndBusiness.getBusinessTaxId());
        assertEquals(dummyPDNDImpresa().getBusinessName(), pdndBusiness.getBusinessName());
    }

    @Test
    void retrieveinstitutionbytaxCodeTest_whenThrowsException() {

        // given
        String taxCode = "taxCode";
        String encTaxCode = "TEST-STRING";

        when(DataEncryptionUtils.encrypt(any())).thenReturn(encTaxCode);
        when(pdndCacheableService.getEncryptedPDNDImpresa(any(), any())).thenReturn(encTaxCode);

        // when
        PDNDBusiness pdndBusiness = pdndInfoCamereConnector.retrieveInstitutionPdndByTaxCode(taxCode, "prod-test");

        // then
        assertNull(pdndBusiness);
    }

    @Test
    void testRetrieveInstitutionByTaxCode_nullTaxCode() {

        // given
        String taxCode = null;

        // when
        Executable executable = () -> pdndInfoCamereConnector.retrieveInstitutionPdndByTaxCode(taxCode, null);

        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("TaxCode is required", e.getMessage());
        Mockito.verifyNoInteractions(pdndInfoCamereRestClient);
    }

    private PDNDBusiness dummyPDNDBusiness() {
        PDNDBusiness pdndBusiness = new PDNDBusiness();
        pdndBusiness.setBusinessTaxId("12345678901");
        pdndBusiness.setBusinessName("Dummy Business Name");
        pdndBusiness.setBusinessStatus("Active");
        pdndBusiness.setCity("Milano");
        pdndBusiness.setCciaa("MI123456");
        pdndBusiness.setAddress("Via Montenapoleone 10");
        pdndBusiness.setDigitalAddress("dummy@example.com");
        pdndBusiness.setCounty("MI");
        pdndBusiness.setLegalNature("LLC");
        pdndBusiness.setLegalNatureDescription("Limited Liability Company");
        pdndBusiness.setNRea("MI67890");
        pdndBusiness.setZipCode("20100");
        return pdndBusiness;
    }

    public PDNDImpresa dummyPDNDImpresa() {
        PDNDImpresa pdndImpresa = new PDNDImpresa();
        pdndImpresa.setBusinessTaxId("12345678901");
        pdndImpresa.setBusinessName("Dummy Business Name");
        pdndImpresa.setLegalNature("LLC");
        pdndImpresa.setLegalNatureDescription("Limited Liability Company");
        pdndImpresa.setCciaa("MI123456");
        pdndImpresa.setNRea("MI67890");
        pdndImpresa.setBusinessStatus("Active");
        pdndImpresa.setBusinessAddress(dummyPDNDSedeImpresa());
        pdndImpresa.setDigitalAddress("dummy@example.com");
        return pdndImpresa;
    }

    public PDNDSedeImpresa dummyPDNDSedeImpresa() {
        PDNDSedeImpresa pdndSedeImpresa = new PDNDSedeImpresa();
        pdndSedeImpresa.setToponimoSede("Via");
        pdndSedeImpresa.setViaSede("Montenapoleone");
        pdndSedeImpresa.setNcivicoSede("10");
        pdndSedeImpresa.setZipCode("20100");
        pdndSedeImpresa.setCity("Milano");
        pdndSedeImpresa.setCounty("MI");
        return pdndSedeImpresa;
    }

    private void mockPdndToken() {
        ClientCredentialsResponse clientCredentialsResponse = new ClientCredentialsResponse();
        clientCredentialsResponse.setAccessToken("accessToken");
        when(tokenProviderPDND.getTokenPdnd(any())).thenReturn(clientCredentialsResponse);
    }

    private void mockPdndSecretValue() {
        when(pdndSecretValueResolver.resolve(any())).thenReturn(PdndSecretValue.builder().build());
    }
}

