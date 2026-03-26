package it.pagopa.selfcare.onboarding.connector;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.selfcare.commons.base.utils.Origin;
import it.pagopa.selfcare.onboarding.connector.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.connector.model.institutions.MatchInfoResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.BusinessInfoIC;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.GeographicTaxonomies;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.HomogeneousOrganizationalArea;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.InstitutionProxyInfo;
import it.pagopa.selfcare.onboarding.connector.model.registry_proxy.OrganizationUnit;
import it.pagopa.selfcare.onboarding.connector.rest.client.PartyRegistryProxyRestClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.RegistryProxyMapper;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.RegistryProxyMapperImpl;
import it.pagopa.selfcare.onboarding.connector.rest.model.AooResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.UoResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.institution_pnpg.InstitutionByLegalTaxIdRequest;
import it.pagopa.selfcare.onboarding.connector.rest.model.institution_pnpg.InstitutionByLegalTaxIdRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.TimeZone;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.commons.utils.TestUtils.reflectionEqualsByName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartyRegistryProxyImplTest {

    @InjectMocks
    private PartyRegistryProxyConnectorImpl partyConnector;

    @Mock
    private PartyRegistryProxyRestClient restClientMock;

    @Captor
    ArgumentCaptor<InstitutionByLegalTaxIdRequest> institutionByLegalTaxIdRequestArgumentCaptor;

    private final ObjectMapper mapper;

    @Spy
    private RegistryProxyMapper proxyMapper = new RegistryProxyMapperImpl();

    public PartyRegistryProxyImplTest() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setTimeZone(TimeZone.getDefault());
    }

    private final static AooResponse aooResponse;
    private final static UoResponse uoResponse;

    static {
        aooResponse = new AooResponse();
        aooResponse.setCodAoo("codAoo");
        aooResponse.setId("id");
        aooResponse.setOrigin(Origin.IPA);

        uoResponse = new UoResponse();
        uoResponse.setUniUoCode("codiceUniUo");
        uoResponse.setId("id");
        uoResponse.setOrigin(Origin.IPA);

    }


    @Test
    void getInstitutionProxyInfo(){
        //given
        ProxyInstitutionResponse proxyInstitutionResponse = new ProxyInstitutionResponse();
        proxyInstitutionResponse.setAddress("42 Main St");
        proxyInstitutionResponse.setAoo("Aoo");
        proxyInstitutionResponse.setCategory("Category");
        proxyInstitutionResponse.setDescription("The characteristics of someone or something");
        proxyInstitutionResponse.setDigitalAddress("42 Main St");
        proxyInstitutionResponse.setId("42");
        proxyInstitutionResponse.setO("foo");
        proxyInstitutionResponse.setOrigin("Origin");
        proxyInstitutionResponse.setOriginId("42");
        proxyInstitutionResponse.setOu("Ou");
        proxyInstitutionResponse.setTaxCode("Tax Code");
        proxyInstitutionResponse.setZipCode("21654");
        when(restClientMock.getInstitutionById(any())).thenReturn(proxyInstitutionResponse);
        //when
        InstitutionProxyInfo actualInstitutionById = partyConnector.getInstitutionProxyById("42");
        //then
        reflectionEqualsByName(proxyInstitutionResponse, actualInstitutionById);
        verify(restClientMock).getInstitutionById("42");
    }


    @Test
    void testGetExtByCode() {
        GeographicTaxonomiesResponse geographicTaxonomiesResponse = new GeographicTaxonomiesResponse();
        geographicTaxonomiesResponse.setGeotaxId("Code");
        geographicTaxonomiesResponse.setCountry("GB");
        geographicTaxonomiesResponse.setCountryAbbreviation("GB");
        geographicTaxonomiesResponse.setDescription("The characteristics of someone or something");
        geographicTaxonomiesResponse.setEnable(true);
        geographicTaxonomiesResponse.setIstatCode("");
        geographicTaxonomiesResponse.setProvinceId("Province");
        geographicTaxonomiesResponse.setProvinceAbbreviation("Province Abbreviation");
        geographicTaxonomiesResponse.setRegionId("us-east-2");
        when(restClientMock.getExtByCode(any())).thenReturn(geographicTaxonomiesResponse);
        GeographicTaxonomies actualExtByCode = partyConnector.getExtById("Code");
        assertEquals("Code", actualExtByCode.getGeotaxId());
        assertTrue(actualExtByCode.isEnable());
        assertEquals("The characteristics of someone or something", actualExtByCode.getDescription());
        verify(restClientMock).getExtByCode(any());
    }

    @Test
    void testGetExtByCode2() {
        GeographicTaxonomiesResponse geographicTaxonomiesResponse = new GeographicTaxonomiesResponse();
        geographicTaxonomiesResponse.setGeotaxId("Code");
        geographicTaxonomiesResponse.setCountry("GB");
        geographicTaxonomiesResponse.setCountryAbbreviation("GB");
        geographicTaxonomiesResponse.setDescription("The characteristics of someone or something");
        geographicTaxonomiesResponse.setEnable(false);
        geographicTaxonomiesResponse.setIstatCode("");
        geographicTaxonomiesResponse.setProvinceId("Province");
        geographicTaxonomiesResponse.setProvinceAbbreviation("Province Abbreviation");
        geographicTaxonomiesResponse.setRegionId("us-east-2");
        when(restClientMock.getExtByCode(any())).thenReturn(geographicTaxonomiesResponse);
        GeographicTaxonomies actualExtByCode = partyConnector.getExtById("Code");
        assertEquals("Code", actualExtByCode.getGeotaxId());
        assertFalse(actualExtByCode.isEnable());
        assertEquals("The characteristics of someone or something", actualExtByCode.getDescription());
        verify(restClientMock).getExtByCode(any());
    }

    @Test
    void shouldGetAoo() {
        when(restClientMock.getAooById(anyString()))
                .thenReturn(aooResponse);

        HomogeneousOrganizationalArea aoo = partyConnector.getAooById("example");
        assertEquals(aoo.getCodAoo(), aooResponse.getCodAoo());
        assertEquals(aoo.getId(), aooResponse.getId());
        assertEquals(aoo.getOrigin(), aooResponse.getOrigin());
    }

    @Test
    void shouldGetUo() {
        when(restClientMock.getUoById(anyString()))
                .thenReturn(uoResponse);

        OrganizationUnit uo = partyConnector.getUoById("example");
        assertEquals(uo.getUniUoCode(), uoResponse.getUniUoCode());
        assertEquals(uo.getId(), uoResponse.getId());
        assertEquals(uo.getOrigin(), uoResponse.getOrigin());
    }

    @Test
    void getInstitutionsByUserFiscalCode() {
        // given
        String legalTaxId = "legalTaxId";
        List<BusinessInfoIC> businessPnPGList = List.of(mockInstance(new BusinessInfoIC()), mockInstance(new BusinessInfoIC()));
        InstitutionInfoIC institutionPnPGInfo = mockInstance(new InstitutionInfoIC(), "setBusinesses");
        institutionPnPGInfo.setBusinesses(businessPnPGList);
        InstitutionByLegalTaxIdRequestDto institutionByLegalTaxIdRequestDto = new InstitutionByLegalTaxIdRequestDto();
        institutionByLegalTaxIdRequestDto.setLegalTaxId(legalTaxId);
        InstitutionByLegalTaxIdRequest institutionByLegalTaxIdRequest = new InstitutionByLegalTaxIdRequest();
        institutionByLegalTaxIdRequest.setFilter(institutionByLegalTaxIdRequestDto);
        when(restClientMock.getInstitutionsByUserLegalTaxId(any()))
                .thenReturn(institutionPnPGInfo);
        // when
        InstitutionInfoIC institutions = partyConnector.getInstitutionsByUserFiscalCode(legalTaxId);
        // then
        assertNotNull(institutions);
        assertNotNull(institutions.getRequestDateTime());
        assertNotNull(institutions.getLegalTaxId());
        assertNotNull(institutions.getBusinesses());
        assertEquals(2, institutions.getBusinesses().size());
        assertEquals(institutions.getRequestDateTime(), institutionPnPGInfo.getRequestDateTime());
        assertEquals(institutions.getLegalTaxId(), institutionPnPGInfo.getLegalTaxId());
        assertEquals(institutions.getBusinesses().get(0).getBusinessName(), institutionPnPGInfo.getBusinesses().get(0).getBusinessName());
        assertEquals(institutions.getBusinesses().get(0).getBusinessTaxId(), institutionPnPGInfo.getBusinesses().get(0).getBusinessTaxId());
        assertEquals(institutions.getBusinesses().get(1).getBusinessName(), institutionPnPGInfo.getBusinesses().get(1).getBusinessName());
        assertEquals(institutions.getBusinesses().get(1).getBusinessTaxId(), institutionPnPGInfo.getBusinesses().get(1).getBusinessTaxId());
        verify(restClientMock, times(1))
                .getInstitutionsByUserLegalTaxId(institutionByLegalTaxIdRequest);
        verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void getInstitutionsByUserFiscalCode_taxCodeNull() {
        // given
        String taxCode = null;
        // when
        Executable executable = () -> partyConnector.getInstitutionsByUserFiscalCode(taxCode);
        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("An user's fiscal code is required", e.getMessage());
        verifyNoInteractions(restClientMock);
    }


    @Test
    void matchInstitutionAndUser() {
        // given
        String externalId = "externalId";
        String taxCode = "taxCode";
        MatchInfoResult pnPGMatchInfo = mockInstance(new MatchInfoResult());
        when(restClientMock.matchInstitutionAndUser(anyString(), anyString()))
                .thenReturn(pnPGMatchInfo);
        // when
        MatchInfoResult result = partyConnector.matchInstitutionAndUser(externalId, taxCode);
        // then
        assertNotNull(result);
        assertEquals(result.isVerificationResult(), pnPGMatchInfo.isVerificationResult());
        verify(restClientMock, times(1))
                .matchInstitutionAndUser(externalId, taxCode);
        verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void matchInstitutionAndUser_externalIdNull() {
        // given
        String externalId = null;
        String taxCode = "taxCode";
        // when
        Executable executable = () -> partyConnector.matchInstitutionAndUser(externalId, taxCode);
        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("An institution's external id is required", e.getMessage());
        verifyNoInteractions(restClientMock);
    }

    @Test
    void matchInstitutionAndUser_taxCodeNull() {
        // given
        String externalId = "externalId";
        String taxCode = null;
        // when
        Executable executable = () -> partyConnector.matchInstitutionAndUser(externalId, taxCode);
        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("An user's fiscal code is required", e.getMessage());
        verifyNoInteractions(restClientMock);
    }

    @Test
    void getInstitutionLegalAddress() {
        // given
        String externalId = "externalId";
        InstitutionLegalAddressData data = mockInstance(new InstitutionLegalAddressData());
        when(restClientMock.getInstitutionLegalAddress(anyString()))
                .thenReturn(data);
        // when
        InstitutionLegalAddressData result = partyConnector.getInstitutionLegalAddress(externalId);
        // then
        assertNotNull(result);
        assertEquals(result.getZipCode(), data.getZipCode());
        assertEquals(result.getAddress(), data.getAddress());
        verify(restClientMock, times(1))
                .getInstitutionLegalAddress(externalId);
        verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void getInstitutionLegalAddress_externalIdNull() {
        // given
        String externalId = null;
        // when
        Executable executable = () -> partyConnector.getInstitutionLegalAddress(externalId);
        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("An institution's external id is required", e.getMessage());
        verifyNoInteractions(restClientMock);
    }


}