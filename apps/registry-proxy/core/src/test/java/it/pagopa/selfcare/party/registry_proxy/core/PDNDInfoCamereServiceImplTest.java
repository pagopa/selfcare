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
        when(pdndInfoCamereConnector.retrieveInstitutionsPdndByDescription(anyString(), any())).thenReturn(pdndBusinesses);

        //when
        pdndInfoCamereService.retrieveInstitutionsPdndByDescription(description, "prod-test");

        //then
        assertNotNull(pdndBusinesses);
        assertNotNull(pdndBusinesses.getClass());
        assertEquals(1, pdndBusinesses.size());
        verify(pdndInfoCamereConnector, times(1))
                .retrieveInstitutionsPdndByDescription(any(), any());
        verifyNoMoreInteractions(pdndInfoCamereConnector);
    }

    @Test
    void retrieveInstitutionsByRea() {
        //given
        final String rea = "rea";
        final String county = "county";
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        when(pdndInfoCamereConnector.retrieveInstitutionFromRea(anyString(), anyString(), any())).thenReturn(pdndBusiness);

        //when
        pdndInfoCamereService.retrieveInstitutionFromRea(rea, county, "prod-test");

        //then
        assertNotNull(pdndBusiness);
        verify(pdndInfoCamereConnector, times(1))
                .retrieveInstitutionFromRea(anyString(), anyString(), any());
        verifyNoMoreInteractions(pdndInfoCamereConnector);
    }

    @Test
    void retrieveInstitutionsByRea_nullRea() {
        //given
        String rea = null;
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        when(pdndInfoCamereConnector.retrieveInstitutionFromRea(any(), any(), any())).thenReturn(pdndBusiness);

        //when
        Executable executable = () -> pdndInfoCamereService.retrieveInstitutionFromRea("county", rea, null);

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
        when(pdndInfoCamereConnector.retrieveInstitutionsPdndByDescription(any(), any())).thenReturn(pdndBusinesses);

        //when
        Executable executable = () -> pdndInfoCamereService.retrieveInstitutionsPdndByDescription(description, null);

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
        when(pdndInfoCamereConnector.retrieveInstitutionPdndByTaxCode(anyString(), any())).thenReturn(pdndBusiness);

        //when
        pdndInfoCamereService.retrieveInstitutionPdndByTaxCode(taxCode, "prod-test");

        //then
        assertNotNull(pdndBusiness);
        assertNotNull(pdndBusiness.getClass());
        verify(pdndInfoCamereConnector, times(1))
                .retrieveInstitutionPdndByTaxCode(any(), any());
        verifyNoMoreInteractions(pdndInfoCamereConnector);
    }


    @Test
    void retrieveInstitutionByTaxCode_nullTaxCode() {
        //given
        String taxCode = null;
        PDNDBusiness pdndBusiness = dummyPDNDBusiness();
        when(pdndInfoCamereConnector.retrieveInstitutionPdndByTaxCode(any(), any())).thenReturn(pdndBusiness);

        //when
        Executable executable = () -> pdndInfoCamereService.retrieveInstitutionPdndByTaxCode(taxCode, null);

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
