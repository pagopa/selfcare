package it.pagopa.selfcare.party.registry_proxy.core;


import it.pagopa.selfcare.party.registry_proxy.connector.api.PDNDInfoCamereConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {PDNDInfoCamereServiceImpl.class})
class PDNDInfoCamereServiceImplTest {

    @Autowired
    private PDNDInfoCamereService pdndInfoCamereService;
    @MockBean
    private PDNDInfoCamereConnector pdndInfoCamereConnector;

    @Test
    void retrieveInstitutionsByDescription() {
        //given
        String description = "description";
        List<PDNDBusiness> pdndBusinesses = new ArrayList<>();
        pdndBusinesses.add(dummyPDNDBusiness());
        when(pdndInfoCamereConnector.retrieveInstitutionsPdndByDescription(anyString())).thenReturn(pdndBusinesses);

        //when
        pdndInfoCamereService.retrieveInstitutionsPdndByDescription(description);

        //then
        assertNotNull(pdndBusinesses);
        assertNotNull(pdndBusinesses.getClass());
        assertEquals(1, pdndBusinesses.size());
        verify(pdndInfoCamereConnector, times(1))
                .retrieveInstitutionsPdndByDescription(any());
        verifyNoMoreInteractions(pdndInfoCamereConnector);
    }

    @Test
    void retrieveInstitutionsByRea() {
        //given
        final String rea = "rea";
        final String county = "county";
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        when(pdndInfoCamereConnector.retrieveInstitutionFromRea(anyString(), anyString())).thenReturn(pdndBusiness);

        //when
        pdndInfoCamereService.retrieveInstitutionFromRea(rea, county);

        //then
        assertNotNull(pdndBusiness);
        verify(pdndInfoCamereConnector, times(1))
                .retrieveInstitutionFromRea(anyString(), anyString());
        verifyNoMoreInteractions(pdndInfoCamereConnector);
    }

    @Test
    void retrieveInstitutionsByRea_nullRea() {
        //given
        String rea = null;
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        when(pdndInfoCamereConnector.retrieveInstitutionFromRea(any(), any())).thenReturn(pdndBusiness);

        //when
        Executable executable = () -> pdndInfoCamereService.retrieveInstitutionFromRea("county", rea);

        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Rea is required", e.getMessage());
        Mockito.verifyNoInteractions(pdndInfoCamereConnector);
    }

    @Test
    void retrieveInstitutionsByDescription_nullDescription() {
        //given
        String description = null;
        List<PDNDBusiness> pdndBusinesses = new ArrayList<>();
        pdndBusinesses.add(dummyPDNDBusiness());
        when(pdndInfoCamereConnector.retrieveInstitutionsPdndByDescription(any())).thenReturn(pdndBusinesses);

        //when
        Executable executable = () -> pdndInfoCamereService.retrieveInstitutionsPdndByDescription(description);

        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Description is required", e.getMessage());
        Mockito.verifyNoInteractions(pdndInfoCamereConnector);
    }

    @Test
    void retrieveInstitutionByTaxCode() {
        //given
        String taxCode = "taxCode";
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        when(pdndInfoCamereConnector.retrieveInstitutionPdndByTaxCode(anyString())).thenReturn(pdndBusiness);

        //when
        pdndInfoCamereService.retrieveInstitutionPdndByTaxCode(taxCode);

        //then
        assertNotNull(pdndBusiness);
        assertNotNull(pdndBusiness.getClass());
        verify(pdndInfoCamereConnector, times(1))
                .retrieveInstitutionPdndByTaxCode(any());
        verifyNoMoreInteractions(pdndInfoCamereConnector);
    }

    @Test
    void retrieveInstitutionDetail() {
        //given
        final String taxCode = "taxCode";
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        when(pdndInfoCamereConnector.retrieveInstitutionDetail(anyString())).thenReturn(pdndBusiness);

        //when
        var result = pdndInfoCamereService.retrieveInstitutionDetail(taxCode);

        //then
        assertNotNull(result);
        assertNotNull(result.getClass());
        verify(pdndInfoCamereConnector, times(1))
                .retrieveInstitutionDetail(any());
        verifyNoMoreInteractions(pdndInfoCamereConnector);
    }

    @Test
    void retrieveInstitutionDocument() {
        //given
        final String taxCode = "taxCode";
        final String document = "document";

        //when
        when(pdndInfoCamereConnector.retrieveInstitutionDocument(anyString())).thenReturn(document.getBytes(StandardCharsets.UTF_8));
        var result = pdndInfoCamereService.retrieveInstitutionDocument(taxCode);

        //then
        assertNotNull(result);
        assertNotEquals(0, result.length);
        verify(pdndInfoCamereConnector, times(1))
                .retrieveInstitutionDocument(any());
        verifyNoMoreInteractions(pdndInfoCamereConnector);
    }

    @Test
    void retrieveInstitutionByTaxCode_nullTaxCode() {
        //given
        String taxCode = null;
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        when(pdndInfoCamereConnector.retrieveInstitutionPdndByTaxCode(any())).thenReturn(pdndBusiness);

        //when
        Executable executable = () -> pdndInfoCamereService.retrieveInstitutionPdndByTaxCode(taxCode);

        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("TaxCode is required", e.getMessage());
        Mockito.verifyNoInteractions(pdndInfoCamereConnector);
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

}
