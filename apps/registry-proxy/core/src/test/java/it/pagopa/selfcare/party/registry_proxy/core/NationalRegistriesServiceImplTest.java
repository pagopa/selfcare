package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.NationalRegistriesConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.constant.AdEResultCodeEnum;
import it.pagopa.selfcare.party.registry_proxy.connector.constant.AdEResultDetailEnum;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.BadGatewayException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.InvalidRequestException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {NationalRegistriesServiceImpl.class})
@ExtendWith(SpringExtension.class)
class NationalRegistriesServiceImplTest {
    @MockBean
    private NationalRegistriesConnector nationalRegistriesConnector;

    @Autowired
    private NationalRegistriesServiceImpl nationalRegistriesServiceImpl;


    /**
     * Method under test: {@link NationalRegistriesServiceImpl#getLegalAddress(String)}
     */
    @Test
    void testGetLegalAddress() {
        LegalAddressProfessionalResponse legalAddressProfessionalResponse = new LegalAddressProfessionalResponse();
        legalAddressProfessionalResponse.setAddress("42 Main St");
        legalAddressProfessionalResponse.setDescription("The characteristics of someone or something");
        legalAddressProfessionalResponse.setMunicipality("Municipality");
        legalAddressProfessionalResponse.setProvince("Province");
        legalAddressProfessionalResponse.setZip("21654");

        LegalAddressProfessionalResponse legalAddressProfessionalResponse1 = new LegalAddressProfessionalResponse();
        legalAddressProfessionalResponse1.setAddress("42 Main St");
        legalAddressProfessionalResponse1.setDescription("The characteristics of someone or something");
        legalAddressProfessionalResponse1.setMunicipality("Municipality");
        legalAddressProfessionalResponse1.setProvince("Province");
        legalAddressProfessionalResponse1.setZip("21654");
        LegalAddressResponse legalAddressResponse = mock(LegalAddressResponse.class);
        when(legalAddressResponse.getProfessionalAddress()).thenReturn(legalAddressProfessionalResponse1);
        doNothing().when(legalAddressResponse).setDateTimeExtraction(any());
        doNothing().when(legalAddressResponse).setProfessionalAddress(any());
        doNothing().when(legalAddressResponse).setTaxId(any());
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        legalAddressResponse.setDateTimeExtraction(Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant()));
        legalAddressResponse.setProfessionalAddress(legalAddressProfessionalResponse);
        legalAddressResponse.setTaxId("42");
        when(nationalRegistriesConnector.getLegalAddress(any())).thenReturn(legalAddressResponse);
        LegalAddressProfessionalResponse actualLegalAddress = nationalRegistriesServiceImpl.getLegalAddress("Tax Id");
        assertEquals("42 Main St", actualLegalAddress.getAddress());
        assertEquals("21654", actualLegalAddress.getZip());
        assertEquals("Province", actualLegalAddress.getProvince());
        assertEquals("Municipality", actualLegalAddress.getMunicipality());
        assertEquals("The characteristics of someone or something", actualLegalAddress.getDescription());
        verify(nationalRegistriesConnector).getLegalAddress(any());
        verify(legalAddressResponse, atLeast(1)).getProfessionalAddress();
        verify(legalAddressResponse).setDateTimeExtraction(any());
        verify(legalAddressResponse).setProfessionalAddress(any());
        verify(legalAddressResponse).setTaxId(any());
    }

    @Test
    void testGetLegalAddressWithoutAddress() {
        LegalAddressProfessionalResponse legalAddressProfessionalResponse = new LegalAddressProfessionalResponse();
        LegalAddressResponse legalAddressResponse = mock(LegalAddressResponse.class);
        doNothing().when(legalAddressResponse).setDateTimeExtraction(any());
        doNothing().when(legalAddressResponse).setProfessionalAddress(any());
        doNothing().when(legalAddressResponse).setTaxId(any());
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        legalAddressResponse.setDateTimeExtraction(Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant()));
        legalAddressResponse.setProfessionalAddress(legalAddressProfessionalResponse);
        legalAddressResponse.setTaxId("42");
        when(nationalRegistriesConnector.getLegalAddress(any())).thenReturn(legalAddressResponse);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> nationalRegistriesServiceImpl.getLegalAddress("Tax Id"));
    }

    /**
     * Method under test: {@link NationalRegistriesServiceImpl#verifyLegal(String, String)}
     */
    @Test
    void testVerifyLegal() {
        VerifyLegalResponse verifyLegalResponse = new VerifyLegalResponse();
        verifyLegalResponse.setVerificationResult(true);
        verifyLegalResponse.setVerifyLegalResultCode(AdEResultCodeEnum.CODE_00);
        verifyLegalResponse.setVerifyLegalResultDetail(AdEResultDetailEnum.XX00);
        when(nationalRegistriesConnector.verifyLegal(any(), any())).thenReturn(verifyLegalResponse);
        assertSame(verifyLegalResponse, nationalRegistriesServiceImpl.verifyLegal("42", "42"));
        verify(nationalRegistriesConnector).verifyLegal(any(), any());
    }

    @Test
    void testVerifyLegalError() {
        VerifyLegalResponse verifyLegalResponse = new VerifyLegalResponse();
        verifyLegalResponse.setVerifyLegalResultCode(AdEResultCodeEnum.CODE_02);
        verifyLegalResponse.setVerifyLegalResultDetail(AdEResultDetailEnum.XX03);
        when(nationalRegistriesConnector.verifyLegal(any(), any())).thenReturn(verifyLegalResponse);
        assertThrows(BadGatewayException.class, () -> nationalRegistriesServiceImpl.verifyLegal("42", "42"));
        verify(nationalRegistriesConnector).verifyLegal(any(), any());
    }

    /**
     * Method under test: {@link NationalRegistriesServiceImpl#verifyLegal(String, String)}
     */
    @ParameterizedTest
    @EnumSource(value = AdEResultDetailEnum.class, names = {"XX01", "XX02", "XXXX"})
    void testVerifyLegalInvalidRequest(AdEResultDetailEnum code) {
        VerifyLegalResponse verifyLegalResponse = new VerifyLegalResponse();
        verifyLegalResponse.setVerifyLegalResultCode(AdEResultCodeEnum.CODE_01);
        verifyLegalResponse.setVerifyLegalResultDetail(code);
        when(nationalRegistriesConnector.verifyLegal(any(), any()))
                .thenReturn(verifyLegalResponse);
        assertThrows(InvalidRequestException.class, () -> nationalRegistriesServiceImpl.verifyLegal("42", "42"));
        verify(nationalRegistriesConnector).verifyLegal(any(), any());
    }

    @Test
    void testInstitutionsByLegalTaxId() {
        Businesses businesses = new Businesses();
        businesses.setBusinessList(new ArrayList<>());
        businesses.setLegalTaxId("42");
        businesses.setDateTimeExtraction("2020-03-01");
        when(nationalRegistriesConnector.institutionsByLegalTaxId(any())).thenReturn(businesses);

        assertSame(businesses, nationalRegistriesServiceImpl.institutionsByLegalTaxId("42"));
        verify(nationalRegistriesConnector).institutionsByLegalTaxId(any());
    }

    @Test
    void testInstitutionsByLegalTaxIdNotFound() {
        Businesses businesses = new Businesses();
        businesses.setCode("WSPA_ERR_04");
        when(nationalRegistriesConnector.institutionsByLegalTaxId(any())).thenReturn(businesses);

        Businesses response = nationalRegistriesServiceImpl.institutionsByLegalTaxId("42");
        assertEquals("42", response.getLegalTaxId());
        assertEquals(0, response.getBusinessList().size());
        verify(nationalRegistriesConnector).institutionsByLegalTaxId(any());
    }

    @Test
    void testInstitutionsByLegalTaxIdNotFound2() {
        Businesses businesses = new Businesses();
        businesses.setDescription("LR Not found");
        when(nationalRegistriesConnector.institutionsByLegalTaxId(any())).thenReturn(businesses);

        Businesses response = nationalRegistriesServiceImpl.institutionsByLegalTaxId("42");
        assertEquals("42", response.getLegalTaxId());
        assertEquals(0, response.getBusinessList().size());
        verify(nationalRegistriesConnector).institutionsByLegalTaxId(any());
    }

    @Test
    void testInstitutionsByLegalTaxIdNotFound3() {
        Businesses businesses = new Businesses();
        businesses.setAppName("wspa-lrpf");
        when(nationalRegistriesConnector.institutionsByLegalTaxId(any())).thenReturn(businesses);

        Businesses response = nationalRegistriesServiceImpl.institutionsByLegalTaxId("42");
        assertEquals("42", response.getLegalTaxId());
        assertEquals(0, response.getBusinessList().size());
        verify(nationalRegistriesConnector).institutionsByLegalTaxId(any());
    }

    @Test
    void testInstitutionsByLegalTaxIdNotFound4() {
        Businesses businesses = new Businesses();
        //businesses.setTimestamp("2023-01-27T17:38:18.774");
        businesses.setTimestamp("null");
        businesses.setLegalTaxId("42");
        businesses.setBusinessList(List.of(new Business()));
        when(nationalRegistriesConnector.institutionsByLegalTaxId(any())).thenReturn(businesses);

        Businesses response = nationalRegistriesServiceImpl.institutionsByLegalTaxId(businesses.getLegalTaxId());
        assertEquals(businesses.getLegalTaxId(), response.getLegalTaxId());
        assertEquals(1, response.getBusinessList().size());
        verify(nationalRegistriesConnector).institutionsByLegalTaxId(any());
    }
}

