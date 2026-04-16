package it.pagopa.selfcare.onboarding.web.controller;

import static it.pagopa.selfcare.commons.utils.TestUtils.checkNotNullFields;
import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.web.security.JwtAuthenticationToken;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.connector.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.institutions.InstitutionInfo;
import it.pagopa.selfcare.onboarding.connector.model.institutions.MatchInfoResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.BusinessInfoIC;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.Billing;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.core.InstitutionService;
import it.pagopa.selfcare.onboarding.web.config.WebTestConfig;
import it.pagopa.selfcare.onboarding.web.model.*;
import it.pagopa.selfcare.onboarding.web.model.mapper.*;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(value = {InstitutionController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {InstitutionController.class, WebTestConfig.class, OnboardingResourceMapperImpl.class, OnboardingInstitutionInfoMapperImpl.class, GeographicTaxonomyMapperImpl.class, UserResourceMapperImpl.class, InstitutionResourceMapperImpl.class})
class InstitutionControllerTest {

    private static final String BASE_URL = "/v1/institutions";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    private InstitutionService institutionServiceMock;

    @Test
    void shouldOnboardingProductWithoutGeotax(@Value("classpath:stubs/onboardingProductsDtoWithoutGeo.json") Resource onboardingDto) throws Exception {
        // given
        String institutionId = "institutionId";
        String productId = "productId";
        // when
        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/onboarding", institutionId, productId)
                        .content(onboardingDto.getInputStream().readAllBytes())
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(content().string(emptyString()));
        // then
        verify(institutionServiceMock, times(1))
                .onboardingProduct(any(OnboardingData.class));
        verifyNoMoreInteractions(institutionServiceMock);
    }


    @Test
    void onboardingCompany(@Value("classpath:stubs/onboardingCompanyDto.json") Resource onboardingDto) throws Exception {

        // when
       mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/company/onboarding")
                        .content(onboardingDto.getInputStream().readAllBytes())
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
               .andExpect(content().string(emptyString()));
        // then

        verify(institutionServiceMock, times(1))
                .onboardingProduct(any(OnboardingData.class));
        verifyNoMoreInteractions(institutionServiceMock);
    }


    @Test
    void onboardingSubunit(@Value("classpath:stubs/onboardingSubunitDto.json") Resource onboardingSubunitDto) throws Exception {
        // given

        // when
        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/onboarding")
                        .content(onboardingSubunitDto.getInputStream().readAllBytes())
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(content().string(emptyString()));
        // then
        verify(institutionServiceMock, times(1))
                .onboardingProduct(any(OnboardingData.class));
        verifyNoMoreInteractions(institutionServiceMock);
    }

    @Test
    void getInstitutionGeographicTaxonomy() throws Exception {
        // given
        String institutionId = "institutionId";
        List<GeographicTaxonomy> geographicTaxonomyListMock = List.of(mockInstance(new GeographicTaxonomy()));
        when(institutionServiceMock.getGeographicTaxonomyList(Mockito.anyString()))
                .thenReturn(geographicTaxonomyListMock);
        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/geographic-taxonomy", institutionId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        List<GeographicTaxonomyResource> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertNotNull(response);
        assertEquals(geographicTaxonomyListMock.get(0).getCode(), response.get(0).getCode());
        assertEquals(geographicTaxonomyListMock.get(0).getDesc(), response.get(0).getDesc());
        verify(institutionServiceMock, times(1))
                .getGeographicTaxonomyList(institutionId);
        verifyNoMoreInteractions(institutionServiceMock);

    }


    @Test
    void getGeographicTaxonomyByTaxCodeAndSubunitCode_withTaxCode() throws Exception {
        // given
        String taxCode = "taxCode";
        List<GeographicTaxonomy> geographicTaxonomyListMock = List.of(mockInstance(new GeographicTaxonomy()));
        when(institutionServiceMock.getGeographicTaxonomyList(taxCode, null))
                .thenReturn(geographicTaxonomyListMock);
        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/geographic-taxonomies?taxCode={taxCode}", taxCode)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        List<GeographicTaxonomyResource> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertNotNull(response);
        assertEquals(geographicTaxonomyListMock.get(0).getCode(), response.get(0).getCode());
        assertEquals(geographicTaxonomyListMock.get(0).getDesc(), response.get(0).getDesc());
    }

    @Test
    void getGeographicTaxonomyByTaxCodeAndSubunitCode_withTaxCodeAndSubunitCode() throws Exception {
        // given
        String taxCode = "taxCode";
        String subunitCode = "subunitCode";
        List<GeographicTaxonomy> geographicTaxonomyListMock = List.of(mockInstance(new GeographicTaxonomy()));
        when(institutionServiceMock.getGeographicTaxonomyList(taxCode, subunitCode))
                .thenReturn(geographicTaxonomyListMock);
        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/geographic-taxonomies?taxCode={taxCode}&subunitCode={subunitCode}", taxCode, subunitCode)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        List<GeographicTaxonomyResource> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertNotNull(response);
        assertEquals(geographicTaxonomyListMock.get(0).getCode(), response.get(0).getCode());
        assertEquals(geographicTaxonomyListMock.get(0).getDesc(), response.get(0).getDesc());
    }


    @Test
    void getInstitutionOnboardingInfo_shouldGetInstitutions() throws Exception {
        //given
        InstitutionInfo institutionInfo = mockInstance(new InstitutionInfo());
        institutionInfo.setId(randomUUID().toString());
        String productId = "prod-io";

        InstitutionOnboardingData institutionOnboardingData = new InstitutionOnboardingData();
        institutionOnboardingData.setInstitution(institutionInfo);
        when(institutionServiceMock.getInstitutionOnboardingDataById(institutionInfo.getId(),productId))
                .thenReturn(institutionOnboardingData);
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/onboarding/")
                        .queryParam("institutionId", institutionInfo.getId())
                        .queryParam("productId", productId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        //then
        InstitutionOnboardingInfoResource actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertNotNull(actual);
        assertNotNull(actual.getInstitution());

        assertEquals(institutionInfo.getId(), actual.getInstitution().getId());
        assertEquals(institutionInfo.getInstitutionType(), actual.getInstitution().getInstitutionType());
        assertEquals(institutionInfo.getOrigin(), actual.getInstitution().getOrigin());
    }

    @Test
    void getInstitutions() throws Exception {
        //given
        JwtAuthenticationToken mockPrincipal = mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example")
                .fiscalCode("fiscalCode")
                .build();
        when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);

        InstitutionInfo institutionInfo = mockInstance(new InstitutionInfo(), "setId");
        institutionInfo.setId(randomUUID().toString());
        String productFilter = "prod-io";
        String userId = selfCareUser.getId();
        when(institutionServiceMock.getInstitutions(productFilter, userId))
                .thenReturn(Collections.singletonList(institutionInfo));
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .queryParam("productId", productFilter)
                        .principal(mockPrincipal)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        //then
        List<InstitutionResource> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(institutionInfo.getId(), response.get(0).getId().toString());
        assertEquals(institutionInfo.getExternalId(), response.get(0).getExternalId());
        assertEquals(institutionInfo.getDescription(), response.get(0).getDescription());
        verify(institutionServiceMock, times(1))
                .getInstitutions(productFilter, userId);
        verifyNoMoreInteractions(institutionServiceMock);
    }


    @Test
    void verifyOnboarding() throws Exception {
        final String externalInstitutionId = "externalInstitutionId";
        final String productId = "productId";
        //when
        mvc.perform(MockMvcRequestBuilders
                        .head(BASE_URL + "/{externalInstitutionId}/products/{productId}", externalInstitutionId, productId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent());
    }


    @Test
    void shouldOnboardingVerification() throws Exception {
        //when
        mvc.perform(MockMvcRequestBuilders
                        .head(BASE_URL + "/onboarding")
                        .queryParam("taxCode", "taxCode")
                        .queryParam("productId", "productId")
                        .queryParam("verifyType", VerifyType.INTERNAL.name())
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent());
    }

    @Test
    void verifyOnboardingExternal() throws Exception{
        //given
        String productId = "prod-fd";
        String vatNumber  ="vatNumber";
        String taxCode = "taxCode";
        //when
        mvc.perform(MockMvcRequestBuilders
                        .head(BASE_URL + "/onboarding")
                        .queryParam("taxCode", taxCode)
                        .queryParam("productId", productId)
                        .queryParam("verifyType", VerifyType.EXTERNAL.name())
                        .queryParam("vatNumber", vatNumber)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent());
        //then
        verify(institutionServiceMock, times(1)).checkOrganization(productId, taxCode, vatNumber);
    }
    @Test
    void verifyOnboardingExternal_garantito() throws Exception{
        //given
        String productId = "prod-fd-garantito";
        String vatNumber  ="vatNumber";
        String taxCode = "taxCode";
        //when
        mvc.perform(MockMvcRequestBuilders
                        .head(BASE_URL + "/onboarding")
                        .queryParam("taxCode", taxCode)
                        .queryParam("productId", productId)
                        .queryParam("verifyType", VerifyType.EXTERNAL.name())
                        .queryParam("vatNumber", vatNumber)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent());
        //then
        verify(institutionServiceMock, times(1)).checkOrganization(productId, taxCode, vatNumber);
    }

    @Test
    void verifyOnboarding2() throws Exception {
        //given
        String productId = "prod-fd";
        String taxCode = "taxCode";
        String subunitCode = "subunitCode";
        String origin = "origin";
        String originId = "originId";
        String institutionType = InstitutionType.PA.name();
        //when
        mvc.perform(MockMvcRequestBuilders
                        .head(BASE_URL + "/onboarding")
                        .queryParam("productId", productId)
                        .queryParam("taxCode", taxCode)
                        .queryParam("origin", origin)
                        .queryParam("originId", originId)
                        .queryParam("subunitCode", subunitCode)
                        .queryParam("institutionType", institutionType)
                        .queryParam("verifyType", VerifyType.EXTERNAL.name())
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent());
        //then
        verify(institutionServiceMock, times(1)).verifyOnboarding(productId, taxCode, origin, originId, subunitCode, institutionType);
    }
    @Test
    void getInstitutionsByUserId() throws Exception {
        //given
        JwtAuthenticationToken mockPrincipal = mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example")
                .fiscalCode("fiscalCode")
                .build();
        when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);

        List<BusinessInfoIC> businessInfoICSmock = List.of(mockInstance(new BusinessInfoIC()));
        InstitutionInfoIC institutionInfoICmock = mockInstance(new InstitutionInfoIC(), "setBusinesses");
        institutionInfoICmock.setBusinesses(businessInfoICSmock);


        when(institutionServiceMock.getInstitutionsByUser(selfCareUser.getFiscalCode()))
                .thenReturn(institutionInfoICmock);
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/from-infocamere/")
                        .principal(mockPrincipal)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        InstitutionResourceIC response = objectMapper.readValue(
                result.getResponse().getContentAsString(), InstitutionResourceIC.class);
        assertNotNull(response);
        assertEquals(institutionInfoICmock.getBusinesses().get(0).getBusinessName(), response.getBusinesses().get(0).getBusinessName());
        assertEquals(institutionInfoICmock.getBusinesses().get(0).getBusinessTaxId(), response.getBusinesses().get(0).getBusinessTaxId());
        assertEquals(institutionInfoICmock.getLegalTaxId(), response.getLegalTaxId());
        assertEquals(institutionInfoICmock.getRequestDateTime(), response.getRequestDateTime());
    }

    @Test
    void matchInstitutionAndUser_badRequest() throws Exception {
        //given
        String taxCode = "taxCode";
        VerificationMatchRequest verificationMatchRequest = new VerificationMatchRequest();
        verificationMatchRequest.setTaxCode(taxCode);
        String jsonBody = objectMapper.writeValueAsString(verificationMatchRequest);
        //when
        mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/verification/match")
                        .content(jsonBody)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void matchInstitutionAndUser_ok(@Value("classpath:stubs/userDto.json") Resource userDto) throws Exception {
        //given
        String taxCode = "taxCode";
        VerificationMatchRequest verificationMatchRequest = new VerificationMatchRequest();
        verificationMatchRequest.setTaxCode(taxCode);
        verificationMatchRequest.setUserDto(objectMapper.readValue(userDto.getInputStream(), UserDto.class));
        String jsonBody = objectMapper.writeValueAsString(verificationMatchRequest);

        MatchInfoResult matchInfo = mockInstance(new MatchInfoResult(), "setVerificationResult");
        matchInfo.setVerificationResult(true);

        when(institutionServiceMock.matchInstitutionAndUser(any(), any()))
                .thenReturn(matchInfo);
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/verification/match")
                        .content(jsonBody)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        MatchInfoResultResource response = objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfoResultResource.class);
        assertNotNull(response);
        assertEquals(response.isVerificationResult(), matchInfo.isVerificationResult());
    }

    @Test
    void postVerificationLegalAddress() throws Exception {
        //given

        String taxCode = "externalId";
        VerificationLegalAddressRequest verificationLegalAddressRequest = new VerificationLegalAddressRequest();
        verificationLegalAddressRequest.setTaxCode(taxCode);
        String jsonBody = objectMapper.writeValueAsString(verificationLegalAddressRequest);

        InstitutionLegalAddressData data = mockInstance(new InstitutionLegalAddressData());

        when(institutionServiceMock.getInstitutionLegalAddress(taxCode))
                .thenReturn(data);
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/verification/legal-address")
                        .content(jsonBody)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        InstitutionLegalAddressResource response = objectMapper
                .readValue(result.getResponse().getContentAsString(), InstitutionLegalAddressResource.class);

        assertNotNull(response);
        assertEquals(response.getAddress(), data.getAddress());
        assertEquals(response.getZipCode(), data.getZipCode());
    }

    @Test
    void getInstitutionOnboardingInfoResource() throws Exception {
        //given
        String institutionId = "institutionId";
        String productId = "productId";
        InstitutionInfo institutionInfoMock = mockInstance(new InstitutionInfo());
        institutionInfoMock.setBilling(mockInstance(new Billing()));
        InstitutionOnboardingData onBoardingDataMock = mockInstance(new InstitutionOnboardingData());
        onBoardingDataMock.setInstitution(institutionInfoMock);
        onBoardingDataMock.setGeographicTaxonomies(List.of(mockInstance(new GeographicTaxonomy())));

        when(institutionServiceMock.getInstitutionOnboardingData(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(onBoardingDataMock);
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/products/{productId}/onboarded-institution-info", institutionId, productId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        //then
        InstitutionOnboardingInfoResource response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                InstitutionOnboardingInfoResource.class
        );
        assertNotNull(response);
        assertNotNull(response.getInstitution());

        BillingDataResponseDto responseBillings = response.getInstitution().getBillingData();
        assertEquals(onBoardingDataMock.getInstitution().getBilling().getRecipientCode(), responseBillings.getRecipientCode());
        assertEquals(onBoardingDataMock.getInstitution().getBilling().getPublicServices(), responseBillings.getPublicServices());
        assertEquals(onBoardingDataMock.getInstitution().getBilling().getVatNumber(), responseBillings.getVatNumber());
        assertEquals(onBoardingDataMock.getInstitution().getDigitalAddress(), responseBillings.getDigitalAddress());
        assertEquals(onBoardingDataMock.getInstitution().getTaxCode(), responseBillings.getTaxCode());
        assertEquals(onBoardingDataMock.getInstitution().getAddress(), responseBillings.getRegisteredOffice());
        assertEquals(onBoardingDataMock.getInstitution().getInstitutionType(), response.getInstitution().getInstitutionType());
        checkNotNullFields(response);
        verify(institutionServiceMock, times(1))
                .getInstitutionOnboardingData(institutionId, productId);
        verifyNoMoreInteractions(institutionServiceMock);

    }
}