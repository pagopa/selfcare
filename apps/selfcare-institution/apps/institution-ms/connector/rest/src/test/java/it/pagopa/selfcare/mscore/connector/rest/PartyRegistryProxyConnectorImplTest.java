package it.pagopa.selfcare.mscore.connector.rest;

import feign.FeignException;
import it.pagopa.selfcare.mscore.connector.rest.client.PartyRegistryProxyRestClient;
import it.pagopa.selfcare.mscore.connector.rest.mapper.*;
import it.pagopa.selfcare.mscore.connector.rest.model.geotaxonomy.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.mscore.connector.rest.model.registryproxy.*;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.AreaOrganizzativaOmogenea;
import it.pagopa.selfcare.mscore.model.UnitaOrganizzativa;
import it.pagopa.selfcare.mscore.model.institution.*;
import it.pagopa.selfcare.registry_proxy.generated.openapi.v1.dto.InsuranceCompanyResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.checkNotNullFields;
import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class PartyRegistryProxyConnectorImplTest {
    @InjectMocks
    private PartyRegistryProxyConnectorImpl partyRegistryProxyConnectorImpl;

    @Mock
    private PartyRegistryProxyRestClient partyRegistryProxyRestClient;

    @Spy
    private AooMapper aooMapper = new AooMapperImpl();

    @Spy
    private UoMapper uoMapper = new UoMapperImpl();

    @Spy
    private SaMapper saMapper = new SaMapperImpl();
    @Spy
    private AsMapper asMapper = new AsMapperImpl();

    private final static AooResponse aooResponse;
    private final static UoResponse uoResponse;
    private final static PdndResponse pdndResponse;
    private final static InsuranceCompanyResource asResponse;

    static {
        aooResponse = new AooResponse();
        aooResponse.setCodAoo("codAoo");
        aooResponse.setId("id");
        aooResponse.setOrigin(Origin.IPA);

        uoResponse = new UoResponse();
        uoResponse.setCodiceUniUo("codiceUniUo");
        uoResponse.setId("id");
        uoResponse.setOrigin(Origin.IPA);
        pdndResponse = mockInstance(new PdndResponse());
        asResponse = mockInstance(new InsuranceCompanyResource());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionById(String)}
     */
    @Test
    void testGetInstitutionById() {
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
        when(partyRegistryProxyRestClient.getInstitutionById(any())).thenReturn(proxyInstitutionResponse);
        InstitutionProxyInfo actualInstitutionById = partyRegistryProxyConnectorImpl.getInstitutionById("42");
        assertEquals("42 Main St", actualInstitutionById.getAddress());
        assertEquals("21654", actualInstitutionById.getZipCode());
        assertEquals("Tax Code", actualInstitutionById.getTaxCode());
        assertEquals("Ou", actualInstitutionById.getOu());
        assertEquals("42", actualInstitutionById.getOriginId());
        assertEquals("Origin", actualInstitutionById.getOrigin());
        assertEquals("foo", actualInstitutionById.getO());
        assertEquals("42", actualInstitutionById.getId());
        assertEquals("42 Main St", actualInstitutionById.getDigitalAddress());
        assertEquals("The characteristics of someone or something", actualInstitutionById.getDescription());
        assertEquals("Category", actualInstitutionById.getCategory());
        assertEquals("Aoo", actualInstitutionById.getAoo());
        verify(partyRegistryProxyRestClient).getInstitutionById(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionById(String)}
     */
    @Test
    void testGetInstitutionById2() {
        when(partyRegistryProxyRestClient.getInstitutionById(any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class, () -> partyRegistryProxyConnectorImpl.getInstitutionById("42"));
        verify(partyRegistryProxyRestClient).getInstitutionById(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionById(String)}
     */
    @Test
    void testGetInstitutionById3() {
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
        when(partyRegistryProxyRestClient.getInstitutionById(any())).thenReturn(proxyInstitutionResponse);
        InstitutionProxyInfo actualInstitutionById = partyRegistryProxyConnectorImpl.getInstitutionById("42");
        assertEquals("42 Main St", actualInstitutionById.getAddress());
        assertEquals("21654", actualInstitutionById.getZipCode());
        assertEquals("Tax Code", actualInstitutionById.getTaxCode());
        assertEquals("Ou", actualInstitutionById.getOu());
        assertEquals("42", actualInstitutionById.getOriginId());
        assertEquals("Origin", actualInstitutionById.getOrigin());
        assertEquals("foo", actualInstitutionById.getO());
        assertEquals("42", actualInstitutionById.getId());
        assertEquals("42 Main St", actualInstitutionById.getDigitalAddress());
        assertEquals("The characteristics of someone or something", actualInstitutionById.getDescription());
        assertEquals("Category", actualInstitutionById.getCategory());
        assertEquals("Aoo", actualInstitutionById.getAoo());
        verify(partyRegistryProxyRestClient).getInstitutionById(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionById(String)}
     */
    @Test
    void testGetInstitutionById4() {
        when(partyRegistryProxyRestClient.getInstitutionById(any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class, () -> partyRegistryProxyConnectorImpl.getInstitutionById("42"));
        verify(partyRegistryProxyRestClient).getInstitutionById(any());
    }

    @Test
    void testGetInstitutionById5() {
        FeignException feignException = mock(FeignException.class);
        when(partyRegistryProxyRestClient.getInstitutionById(any()))
                .thenThrow(feignException);
        assertThrows(MsCoreException.class, () -> partyRegistryProxyConnectorImpl.getInstitutionById("42"));
        verify(partyRegistryProxyRestClient).getInstitutionById(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getCategory(String, String)}
     */
    @Test
    void testGetCategory() {
        ProxyCategoryResponse proxyCategoryResponse = new ProxyCategoryResponse();
        proxyCategoryResponse.setCode("Code");
        proxyCategoryResponse.setKind("Kind");
        proxyCategoryResponse.setName("Name");
        proxyCategoryResponse.setOrigin("Origin");
        when(partyRegistryProxyRestClient.getCategory(any(), any())).thenReturn(proxyCategoryResponse);
        CategoryProxyInfo actualCategory = partyRegistryProxyConnectorImpl.getCategory("Origin", "Code");
        assertEquals("Code", actualCategory.getCode());
        assertEquals("Origin", actualCategory.getOrigin());
        assertEquals("Name", actualCategory.getName());
        assertEquals("Kind", actualCategory.getKind());
        verify(partyRegistryProxyRestClient).getCategory(any(), any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getCategory(String, String)}
     */
    @Test
    void testGetCategory2() {
        when(partyRegistryProxyRestClient.getCategory(any(), any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class,
                () -> partyRegistryProxyConnectorImpl.getCategory("Origin", "Code"));
        verify(partyRegistryProxyRestClient).getCategory(any(), any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getCategory(String, String)}
     */
    @Test
    void testGetCategory3() {
        ProxyCategoryResponse proxyCategoryResponse = new ProxyCategoryResponse();
        proxyCategoryResponse.setCode("Code");
        proxyCategoryResponse.setKind("Kind");
        proxyCategoryResponse.setName("Name");
        proxyCategoryResponse.setOrigin("Origin");
        when(partyRegistryProxyRestClient.getCategory(any(), any())).thenReturn(proxyCategoryResponse);
        CategoryProxyInfo actualCategory = partyRegistryProxyConnectorImpl.getCategory("Origin", "Code");
        assertEquals("Code", actualCategory.getCode());
        assertEquals("Origin", actualCategory.getOrigin());
        assertEquals("Name", actualCategory.getName());
        assertEquals("Kind", actualCategory.getKind());
        verify(partyRegistryProxyRestClient).getCategory(any(), any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getCategory(String, String)}
     */
    @Test
    void testGetCategory4() {
        when(partyRegistryProxyRestClient.getCategory(any(), any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class,
                () -> partyRegistryProxyConnectorImpl.getCategory("Origin", "Code"));
        verify(partyRegistryProxyRestClient).getCategory(any(), any());
    }

    @Test
    void testGetCategory5() {
        FeignException feignException = mock(FeignException.class);
        when(partyRegistryProxyRestClient.getCategory(any(), any()))
                .thenThrow(feignException);
        assertThrows(MsCoreException.class, () -> partyRegistryProxyConnectorImpl.getCategory("origin", "code"));
        verify(partyRegistryProxyRestClient).getCategory(any(), any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionsByLegal(String)}
     */
    @Test
    void testGetInstitutionsByLegal() {
        InstitutionsByLegalResponse institutionsByLegalResponse = new InstitutionsByLegalResponse();
        institutionsByLegalResponse.setBusinesses(new ArrayList<>());
        institutionsByLegalResponse.setLegalTaxId("42");
        institutionsByLegalResponse.setRequestDateTime("2020-03-01");
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any()))
                .thenReturn(institutionsByLegalResponse);
        assertTrue(partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42").isEmpty());
        verify(partyRegistryProxyRestClient).getInstitutionsByLegal(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionsByLegal(String)}
     */
    @Test
    void testGetInstitutionsByLegal2() {
        Institutions institutions = new Institutions();
        institutions.setBusinessName("Business Name");
        institutions.setBusinessTaxId("42");

        ArrayList<Institutions> institutionsList = new ArrayList<>();
        institutionsList.add(institutions);

        InstitutionsByLegalResponse institutionsByLegalResponse = new InstitutionsByLegalResponse();
        institutionsByLegalResponse.setBusinesses(institutionsList);
        institutionsByLegalResponse.setLegalTaxId("42");
        institutionsByLegalResponse.setRequestDateTime("2020-03-01");
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any()))
                .thenReturn(institutionsByLegalResponse);
        List<InstitutionByLegal> actualInstitutionsByLegal = partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42");
        assertEquals(1, actualInstitutionsByLegal.size());
        InstitutionByLegal getResult = actualInstitutionsByLegal.get(0);
        assertEquals("Business Name", getResult.getBusinessName());
        assertEquals("42", getResult.getBusinessTaxId());
        verify(partyRegistryProxyRestClient).getInstitutionsByLegal(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionsByLegal(String)}
     */
    @Test
    void testGetInstitutionsByLegal3() {
        Institutions institutions = new Institutions();
        institutions.setBusinessName("Business Name");
        institutions.setBusinessTaxId("42");

        Institutions institutions1 = new Institutions();
        institutions1.setBusinessName("Business Name");
        institutions1.setBusinessTaxId("42");

        ArrayList<Institutions> institutionsList = new ArrayList<>();
        institutionsList.add(institutions1);
        institutionsList.add(institutions);

        InstitutionsByLegalResponse institutionsByLegalResponse = new InstitutionsByLegalResponse();
        institutionsByLegalResponse.setBusinesses(institutionsList);
        institutionsByLegalResponse.setLegalTaxId("42");
        institutionsByLegalResponse.setRequestDateTime("2020-03-01");
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any()))
                .thenReturn(institutionsByLegalResponse);
        List<InstitutionByLegal> actualInstitutionsByLegal = partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42");
        assertEquals(2, actualInstitutionsByLegal.size());
        InstitutionByLegal getResult = actualInstitutionsByLegal.get(0);
        assertEquals("42", getResult.getBusinessTaxId());
        InstitutionByLegal getResult1 = actualInstitutionsByLegal.get(1);
        assertEquals("42", getResult1.getBusinessTaxId());
        assertEquals("Business Name", getResult1.getBusinessName());
        assertEquals("Business Name", getResult.getBusinessName());
        verify(partyRegistryProxyRestClient).getInstitutionsByLegal(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionsByLegal(String)}
     */
    @Test
    void testGetInstitutionsByLegal4() {
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class, () -> partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42"));
        verify(partyRegistryProxyRestClient).getInstitutionsByLegal(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionsByLegal(String)}
     */
    @Test
    void testGetInstitutionsByLegal5() {
        InstitutionsByLegalResponse institutionsByLegalResponse = new InstitutionsByLegalResponse();
        institutionsByLegalResponse.setBusinesses(new ArrayList<>());
        institutionsByLegalResponse.setLegalTaxId("42");
        institutionsByLegalResponse.setRequestDateTime("2020-03-01");
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any()))
                .thenReturn(institutionsByLegalResponse);
        assertTrue(partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42").isEmpty());
        verify(partyRegistryProxyRestClient).getInstitutionsByLegal(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionsByLegal(String)}
     */
    @Test
    void testGetInstitutionsByLegal6() {
        Institutions institutions = new Institutions();
        institutions.setBusinessName("Business Name");
        institutions.setBusinessTaxId("42");

        ArrayList<Institutions> institutionsList = new ArrayList<>();
        institutionsList.add(institutions);

        InstitutionsByLegalResponse institutionsByLegalResponse = new InstitutionsByLegalResponse();
        institutionsByLegalResponse.setBusinesses(institutionsList);
        institutionsByLegalResponse.setLegalTaxId("42");
        institutionsByLegalResponse.setRequestDateTime("2020-03-01");
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any()))
                .thenReturn(institutionsByLegalResponse);
        List<InstitutionByLegal> actualInstitutionsByLegal = partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42");
        assertEquals(1, actualInstitutionsByLegal.size());
        InstitutionByLegal getResult = actualInstitutionsByLegal.get(0);
        assertEquals("Business Name", getResult.getBusinessName());
        assertEquals("42", getResult.getBusinessTaxId());
        verify(partyRegistryProxyRestClient).getInstitutionsByLegal(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionsByLegal(String)}
     */
    @Test
    void testGetInstitutionsByLegal7() {
        Institutions institutions = new Institutions();
        institutions.setBusinessName("Business Name");
        institutions.setBusinessTaxId("42");

        Institutions institutions1 = new Institutions();
        institutions1.setBusinessName("Business Name");
        institutions1.setBusinessTaxId("42");

        ArrayList<Institutions> institutionsList = new ArrayList<>();
        institutionsList.add(institutions1);
        institutionsList.add(institutions);

        InstitutionsByLegalResponse institutionsByLegalResponse = new InstitutionsByLegalResponse();
        institutionsByLegalResponse.setBusinesses(institutionsList);
        institutionsByLegalResponse.setLegalTaxId("42");
        institutionsByLegalResponse.setRequestDateTime("2020-03-01");
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any()))
                .thenReturn(institutionsByLegalResponse);
        List<InstitutionByLegal> actualInstitutionsByLegal = partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42");
        assertEquals(2, actualInstitutionsByLegal.size());
        InstitutionByLegal getResult = actualInstitutionsByLegal.get(0);
        assertEquals("42", getResult.getBusinessTaxId());
        InstitutionByLegal getResult1 = actualInstitutionsByLegal.get(1);
        assertEquals("42", getResult1.getBusinessTaxId());
        assertEquals("Business Name", getResult1.getBusinessName());
        assertEquals("Business Name", getResult.getBusinessName());
        verify(partyRegistryProxyRestClient).getInstitutionsByLegal(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getInstitutionsByLegal(String)}
     */
    @Test
    void testGetInstitutionsByLegal8() {
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class, () -> partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42"));
        verify(partyRegistryProxyRestClient).getInstitutionsByLegal(any());
    }

    @Test
    void testGetInstitutionsByLegal9() {
        FeignException feignException = mock(FeignException.class);
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any()))
                .thenThrow(feignException);
        assertThrows(MsCoreException.class, () -> partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42"));
        verify(partyRegistryProxyRestClient).getInstitutionsByLegal(any());
    }

    @Test
    void testGetInstitutionsByLegal10() {
        InstitutionsByLegalResponse response = new InstitutionsByLegalResponse();
        when(partyRegistryProxyRestClient.getInstitutionsByLegal(any())).thenReturn(response);
        assertNotNull(partyRegistryProxyConnectorImpl.getInstitutionsByLegal("42"));
    }


    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getLegalAddress(String)}
     */
    @Test
    void testGetLegalAddress() {
        NationalRegistriesProfessionalAddress nationalRegistriesProfessionalAddress = new NationalRegistriesProfessionalAddress();
        nationalRegistriesProfessionalAddress.setAddress("42 Main St");
        nationalRegistriesProfessionalAddress.setZipCode("21654");
        when(partyRegistryProxyRestClient.getLegalAddress(any()))
                .thenReturn(nationalRegistriesProfessionalAddress);
        assertSame(nationalRegistriesProfessionalAddress, partyRegistryProxyConnectorImpl.getLegalAddress("42"));
        verify(partyRegistryProxyRestClient).getLegalAddress(any());
    }

    /**
     * Method under test: {@link PartyRegistryProxyConnectorImpl#getLegalAddress(String)}
     */
    @Test
    void testGetLegalAddress2() {
        when(partyRegistryProxyRestClient.getLegalAddress(any()))
                .thenThrow(new ResourceNotFoundException("An error occurred", "Code"));
        assertThrows(ResourceNotFoundException.class, () -> partyRegistryProxyConnectorImpl.getLegalAddress("id"));
    }

    @Test
    void testGetLegalAddress3() {
        FeignException feignException = mock(FeignException.class);
        when(partyRegistryProxyRestClient.getLegalAddress(any()))
                .thenThrow(feignException);
        assertThrows(MsCoreException.class, () -> partyRegistryProxyConnectorImpl.getLegalAddress("42"));
        verify(partyRegistryProxyRestClient).getLegalAddress(any());
    }

    @Test
    void testGetInstitutionByIdThrow() {
        when(partyRegistryProxyRestClient.getInstitutionById(any())).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> partyRegistryProxyConnectorImpl.getInstitutionById("id"));
        verify(partyRegistryProxyRestClient).getInstitutionById(any());
    }


    @Test
    void getExtByCode() {
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
        when(partyRegistryProxyRestClient.getExtByCode(any())).thenReturn(geographicTaxonomiesResponse);
        GeographicTaxonomies actualExtByCode = partyRegistryProxyConnectorImpl.getExtByCode("Code");
        assertEquals("Code", actualExtByCode.getGeotaxId());
        assertTrue(actualExtByCode.isEnable());
        assertEquals("The characteristics of someone or something", actualExtByCode.getDescription());
        verify(partyRegistryProxyRestClient).getExtByCode(any());
    }

    @Test
    void getExtByCode_notFound() {
        when(partyRegistryProxyRestClient.getExtByCode(any())).thenThrow(new ResourceNotFoundException("", ""));
        assertThrows(ResourceNotFoundException.class, () -> partyRegistryProxyConnectorImpl.getExtByCode("Code"));
    }

    @Test
    void shouldGetAoo() {
        when(partyRegistryProxyRestClient.getAooById(anyString()))
                .thenReturn(aooResponse);

        AreaOrganizzativaOmogenea aoo = partyRegistryProxyConnectorImpl.getAooById("example");
        assertEquals(aoo.getCodAoo(), aooResponse.getCodAoo());
        assertEquals(aoo.getId(), aooResponse.getId());
        assertEquals(aoo.getOrigin(), aooResponse.getOrigin());
    }

    @Test
    void shouldGetUo() {
        when(partyRegistryProxyRestClient.getUoById(anyString()))
                .thenReturn(uoResponse);

        UnitaOrganizzativa uo = partyRegistryProxyConnectorImpl.getUoById("example");
        assertEquals(uo.getCodiceUniUo(), uoResponse.getCodiceUniUo());
        assertEquals(uo.getId(), uoResponse.getId());
        assertEquals(uo.getOrigin(), uoResponse.getOrigin());
    }

    @Test
    void shouldGetSa() {
        when(partyRegistryProxyRestClient.getUoById(anyString()))
                .thenReturn(uoResponse);

        UnitaOrganizzativa uo = partyRegistryProxyConnectorImpl.getUoById("example");
        assertEquals(uo.getCodiceUniUo(), uoResponse.getCodiceUniUo());
        assertEquals(uo.getId(), uoResponse.getId());
        assertEquals(uo.getOrigin(), uoResponse.getOrigin());
    }

    @Test
    void getSAFromAnac() {
        //given
        String taxId = "taxId";
        when(partyRegistryProxyRestClient.getSaByTaxId(anyString())).thenReturn(pdndResponse);
        //when
        SaResource result = partyRegistryProxyConnectorImpl.getSAFromAnac(taxId);
        //then
        checkNotNullFields(result);
        verify(partyRegistryProxyRestClient, times(1)).getSaByTaxId(taxId);
    }

    @Test
    void getSAFromAnacNotFound() {
        //given
        String taxId = "taxId";
        when(partyRegistryProxyRestClient.getSaByTaxId(anyString())).thenThrow(ResourceNotFoundException.class);
        //when
        Executable executable = () -> partyRegistryProxyConnectorImpl.getSAFromAnac(taxId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(partyRegistryProxyRestClient, times(1)).getSaByTaxId(taxId);
    }

    @Test
    void getASFromIvass() {
        //given
        ResponseEntity<InsuranceCompanyResource> response = new ResponseEntity<>(asResponse, HttpStatus.FOUND);
        String ivassCode = "ivassCode";
        when(partyRegistryProxyRestClient._searchByOriginIdUsingGET(anyString())).thenReturn(response);
        //when
        ASResource result = partyRegistryProxyConnectorImpl.getASFromIvass(ivassCode);
        //then
        checkNotNullFields(result);
        verify(partyRegistryProxyRestClient, times(1))._searchByOriginIdUsingGET(ivassCode);
    }

    @Test
    void getASFromIvassNotFound() {
        //given
        String ivassCode = "ivassCode";
        when(partyRegistryProxyRestClient._searchByOriginIdUsingGET(anyString())).thenReturn(null);

        //when
        Executable executable = () -> partyRegistryProxyConnectorImpl.getASFromIvass(ivassCode);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(partyRegistryProxyRestClient, times(1))._searchByOriginIdUsingGET(ivassCode);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenIvassCodeNotFound() {
        String ivassCode = "12345";

        when(partyRegistryProxyRestClient._searchByOriginIdUsingGET(ivassCode)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> partyRegistryProxyConnectorImpl.getASFromIvass(ivassCode));
    }
}

