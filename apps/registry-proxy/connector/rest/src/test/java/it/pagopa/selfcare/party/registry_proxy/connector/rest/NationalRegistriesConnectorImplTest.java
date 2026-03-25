package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.*;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.NationalRegistriesRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.AdELegalOKDto;
import it.pagopa.selfcare.party.registry_proxy.connector.constant.AdEResultCodeEnum;
import it.pagopa.selfcare.party.registry_proxy.connector.constant.AdEResultDetailEnum;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.GetAddressRegistroImpreseOKDto;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.ProfessionalAddressDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {NationalRegistriesConnectorImpl.class})
@ExtendWith(SpringExtension.class)
class NationalRegistriesConnectorImplTest {
    @Autowired
    private NationalRegistriesConnectorImpl nationalRegistriesConnectorImpl;

    @MockBean
    private NationalRegistriesRestClient nationalRegistriesRestClient;


    @Test
    void testBusinessesByLegal() {

        Businesses businesses = new Businesses();
        businesses.setBusinessList(new ArrayList<>());
        businesses.setLegalTaxId("42");
        businesses.setDateTimeExtraction("2020-03-01");
        when(nationalRegistriesRestClient.getLegalInstitutions(any())).thenReturn(businesses);

        assertSame(businesses, nationalRegistriesConnectorImpl.institutionsByLegalTaxId("42"));
    }

    /**
     * Method under test: {@link NationalRegistriesConnectorImpl#getLegalAddress(String)}
     */
    @Test
    void testGetLegalAddress() {
        ProfessionalAddressDto legalAddressProfessionalResponse = new ProfessionalAddressDto();
        legalAddressProfessionalResponse.setAddress("42 Main St");
        legalAddressProfessionalResponse.setDescription("The characteristics of someone or something");
        legalAddressProfessionalResponse.setMunicipality("Municipality");
        legalAddressProfessionalResponse.setProvince("Province");
        legalAddressProfessionalResponse.setZip("21654");

        GetAddressRegistroImpreseOKDto legalAddressResponse = new GetAddressRegistroImpreseOKDto();
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        legalAddressResponse
                .setDateTimeExtraction(Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant()));
        legalAddressResponse.setProfessionalAddress(legalAddressProfessionalResponse);
        legalAddressResponse.setTaxId("42");
        when(nationalRegistriesRestClient.getLegalAddress(any()))
                .thenReturn(legalAddressResponse);
        LegalAddressResponse actualLegalAddress = nationalRegistriesConnectorImpl
                .getLegalAddress("Tax Code");
        assertEquals("42 Main St", actualLegalAddress.getProfessionalAddress().getAddress());
        assertEquals("21654", actualLegalAddress.getProfessionalAddress().getZip());
        verify(nationalRegistriesRestClient).getLegalAddress(any());
    }

    /**
     * Method under test: {@link NationalRegistriesConnectorImpl#getLegalAddress(String)}
     */
    @Test
    void testGetLegalAddress3() {

        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        getAddressRegistroImpreseOKDto.setDateTimeExtraction(fromResult);
        getAddressRegistroImpreseOKDto.setTaxId("42");
        when(nationalRegistriesRestClient.getLegalAddress(any()))
                .thenReturn(getAddressRegistroImpreseOKDto);
        LegalAddressResponse response = nationalRegistriesConnectorImpl.getLegalAddress("Tax Code");
        Assertions.assertNull(response.getProfessionalAddress());
        Assertions.assertEquals("42", response.getTaxId());
    }

    /**
     * Method under test: {@link NationalRegistriesConnectorImpl#getLegalAddress(String)}
     */
    @Test
    void testGetLegalAddress4() {
        ProfessionalAddressDto professionalAddressDto = new ProfessionalAddressDto();
        professionalAddressDto.setAddress("42 Main St");
        professionalAddressDto.setDescription("The characteristics of someone or something");
        professionalAddressDto.setMunicipality("Municipality");
        professionalAddressDto.setProvince("Province");
        professionalAddressDto.setZip("21654");

        GetAddressRegistroImpreseOKDto getAddressRegistroImpreseOKDto = new GetAddressRegistroImpreseOKDto();
        LocalDateTime atStartOfDayResult = LocalDate.of(1970, 1, 1).atStartOfDay();
        Date fromResult = Date.from(atStartOfDayResult.atZone(ZoneId.of("UTC")).toInstant());
        getAddressRegistroImpreseOKDto.setDateTimeExtraction(fromResult);
        getAddressRegistroImpreseOKDto.setProfessionalAddress(professionalAddressDto);
        getAddressRegistroImpreseOKDto.setTaxId("42");
        when(nationalRegistriesRestClient.getLegalAddress(any()))
                .thenReturn(getAddressRegistroImpreseOKDto);
        LegalAddressResponse actualLegalAddress = nationalRegistriesConnectorImpl.getLegalAddress("Tax Code");
        assertSame(fromResult, actualLegalAddress.getDateTimeExtraction());
        assertEquals("42", actualLegalAddress.getTaxId());
        LegalAddressProfessionalResponse professionalAddress = actualLegalAddress.getProfessionalAddress();
        assertEquals("42 Main St", professionalAddress.getAddress());
        assertEquals("21654", professionalAddress.getZip());
        assertEquals("Province", professionalAddress.getProvince());
        assertEquals("Municipality", professionalAddress.getMunicipality());
        assertEquals("The characteristics of someone or something", professionalAddress.getDescription());
        verify(nationalRegistriesRestClient).getLegalAddress(any());
    }

    /**
     * Method under test: {@link NationalRegistriesConnectorImpl#verifyLegal(String, String)}
     */
    @Test
    void testVerifyLegal() {
        AdELegalOKDto adELegalOKDto = new AdELegalOKDto();
        adELegalOKDto.setResultCode(AdEResultCodeEnum.CODE_00);
        adELegalOKDto.setResultDetail(AdEResultDetailEnum.XX00);
        adELegalOKDto.setResultDetailMessage("Detail Message");
        adELegalOKDto.setVerificationResult(true);
        when(nationalRegistriesRestClient.verifyLegal(any())).thenReturn(adELegalOKDto);
        VerifyLegalResponse actualVerifyLegalResult = nationalRegistriesConnectorImpl.verifyLegal("42", "42");
        assertEquals("00", actualVerifyLegalResult.getVerifyLegalResultCode().getValue());
        assertTrue(actualVerifyLegalResult.isVerificationResult());
        assertEquals("XX00", actualVerifyLegalResult.getVerifyLegalResultDetail().getValue());
        verify(nationalRegistriesRestClient).verifyLegal(any());
    }

    /**
     * Method under test: {@link NationalRegistriesConnectorImpl#verifyLegal(String, String)}
     */
    @Test
    void testVerifyLegalWithoutVerificationCode() {
        AdELegalOKDto adELegalOKDto = new AdELegalOKDto();
        adELegalOKDto.setResultCode(AdEResultCodeEnum.CODE_00);
        adELegalOKDto.setResultDetail(AdEResultDetailEnum.XX00);
        adELegalOKDto.setResultDetailMessage("Detail Message");
        when(nationalRegistriesRestClient.verifyLegal(any())).thenReturn(adELegalOKDto);
        VerifyLegalResponse actualVerifyLegalResult = nationalRegistriesConnectorImpl.verifyLegal("42", "42");
        assertEquals("00", actualVerifyLegalResult.getVerifyLegalResultCode().getValue());
        assertFalse(actualVerifyLegalResult.isVerificationResult());
        assertEquals("XX00", actualVerifyLegalResult.getVerifyLegalResultDetail().getValue());
        verify(nationalRegistriesRestClient).verifyLegal(any());
    }
}

