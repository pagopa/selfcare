package it.pagopa.selfcare.onboarding.web.controller;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
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
import it.pagopa.selfcare.commons.base.utils.ProductId;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.institutions.ManagerVerification;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.InstitutionOnboarding;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.core.InstitutionService;
import it.pagopa.selfcare.onboarding.web.config.WebTestConfig;
import it.pagopa.selfcare.onboarding.web.model.*;
import it.pagopa.selfcare.onboarding.web.model.mapper.GeographicTaxonomyMapperImpl;
import it.pagopa.selfcare.onboarding.web.model.mapper.InstitutionResourceMapperImpl;
import it.pagopa.selfcare.onboarding.web.model.mapper.OnboardingInstitutionInfoMapperImpl;
import it.pagopa.selfcare.onboarding.web.model.mapper.OnboardingResourceMapperImpl;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(value = {InstitutionV2Controller.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {InstitutionV2Controller.class, WebTestConfig.class, OnboardingResourceMapperImpl.class, OnboardingInstitutionInfoMapperImpl.class, GeographicTaxonomyMapperImpl.class, InstitutionResourceMapperImpl.class})
class InstitutionV2ControllerTest {

    private static final String BASE_URL = "/v2/institutions";

    @Autowired
    protected MockMvc mvc;

    @MockBean
    private InstitutionService institutionServiceMock;

    @InjectMocks
    private InstitutionV2Controller institutionController;

    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    void onboardingProductAsync(@Value("classpath:stubs/onboardingProductsDtoWithoutGeo.json") Resource onboardingDto) throws Exception {
        // given
        String institutionId = "institutionId";
        String productId = "productId";
        // when
        mvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/onboarding", institutionId, productId).content(onboardingDto.getInputStream().readAllBytes()).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isCreated()).andExpect(content().string(emptyString()));
        // then
        verify(institutionServiceMock, times(1)).onboardingProductV2(any(OnboardingData.class));
        verify(institutionServiceMock, times(1)).validateOnboardingByProductOrInstitutionTaxCode(anyString(), anyString());
        verifyNoMoreInteractions(institutionServiceMock);
    }

    @Test
    void onboardingProductForAggregatorAsync(@Value("classpath:stubs/onboardingProductsDtoWithAggregates.json") Resource onboardingDto) throws Exception {
        // given
        String institutionId = "institutionId";
        String productId = "productId";
        // when
        mvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/onboarding", institutionId, productId).content(onboardingDto.getInputStream().readAllBytes()).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isCreated()).andExpect(content().string(emptyString()));
        // then
        verify(institutionServiceMock, times(1)).onboardingPaAggregator(any(OnboardingData.class));
        verify(institutionServiceMock, times(1)).validateOnboardingByProductOrInstitutionTaxCode(anyString(), anyString());
        verifyNoMoreInteractions(institutionServiceMock);
    }


    @Test
    void onboardingCompany(@Value("classpath:stubs/onboardingCompanyDto.json") Resource onboardingDto) throws Exception {
        //given
        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example").fiscalCode("fiscalCode").build();
        Mockito.when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);
        // when
        mvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/company/onboarding").principal(mockPrincipal).content(onboardingDto.getInputStream().readAllBytes()).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isCreated()).andExpect(content().string(emptyString()));
        // then

        verify(institutionServiceMock, times(1)).onboardingCompanyV2(any(OnboardingData.class), anyString());
        verifyNoMoreInteractions(institutionServiceMock);
    }

    /**
     * Method under test: {@link InstitutionV2Controller#getInstitution(String, String, String, String, String)}
     */
    @Test
    void getByFilters() throws Exception {

        // when
        final String origin = "origin";
        final String originId = "originId";
        final String productId = "productId";
        Institution institution = new Institution();
        institution.setDescription("description");
        institution.setInstitutionType(InstitutionType.PA);
        institution.setId(UUID.randomUUID().toString());
        when(institutionServiceMock.getByFilters(productId, null, origin, originId, null)).thenReturn(List.of(institution));
        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "?origin={origin}&originId={originId}&productId={productId}", origin, originId, productId).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isOk()).andReturn();

        // then
        List<Institution> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(institution.getDescription(), response.get(0).getDescription());
        assertEquals(institution.getId(), response.get(0).getId());
    }

    /**
     * Method under test: {@link InstitutionV2Controller#getInstitution(String, String, String, String, String)}
     */
    @Test
    void getByFiltersBadRequest() throws Exception {
        // when
        final String origin = "origin";
        final String originId = "originId";
        // when
        mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "?origin={origin}&originId={originId}", origin, originId).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isBadRequest());

    }

    @Test
    void verifyAggregatesCsvSuccess() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("aggregates", "hello.csv", "text/csv", "Hello world".getBytes());

        // When
        mvc.perform(MockMvcRequestBuilders.multipart(BASE_URL + "/onboarding/aggregation/verification").file(file).param("productId", ProductId.PROD_PAGOPA.getValue()).contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(status().isOk());

        // Then
        verify(institutionServiceMock, times(1)).validateAggregatesCsv(any(MultipartFile.class), anyString());
        verifyNoMoreInteractions(institutionServiceMock);
    }

    @Test
    void verifyAggregatesWrongFile() throws Exception {
        // Given
        MultipartFile file = new MockMultipartFile("file", "hello.tsxt", MediaType.TEXT_PLAIN_VALUE, "Hello, World!".getBytes());

        // When
        mvc.perform(MockMvcRequestBuilders.multipart(BASE_URL + "/onboarding/aggregation/verification").file("aggregates", file.getBytes()).param("productId", ProductId.PROD_PAGOPA.getValue()).contentType(MediaType.MULTIPART_FORM_DATA_VALUE)).andExpect(status().isInternalServerError());

        // Then
        verifyNoInteractions(institutionServiceMock);
    }

    @Test
    void verifyManager_success() throws Exception {
        // given
        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example").fiscalCode("fiscalCode").build();
        Mockito.when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);

        VerifyManagerRequest verifyManagerRequest = new VerifyManagerRequest();
        verifyManagerRequest.setCompanyTaxCode("taxCode");

        ManagerVerification managerVerification = new ManagerVerification();
        managerVerification.setOrigin("INFOCAMERE");
        managerVerification.setCompanyName("CompanyName");
        when(institutionServiceMock.verifyManager(anyString(), anyString())).thenReturn(managerVerification);

        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/company/verify-manager").content(objectMapper.writeValueAsString(verifyManagerRequest)).principal(mockPrincipal).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isOk()).andReturn();

        // then
        VerifyManagerResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), VerifyManagerResponse.class);
        assertNotNull(response);
        assertEquals("INFOCAMERE", response.getOrigin());
        assertEquals("CompanyName", response.getCompanyName());
        verify(institutionServiceMock, times(1)).verifyManager(anyString(), anyString());
        verifyNoMoreInteractions(institutionServiceMock);
    }

    @Test
    void verifyManager_invalidRequest() throws Exception {
        // given
        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example").fiscalCode("fiscalCode").build();
        Mockito.when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);

        VerifyManagerRequest request = new VerifyManagerRequest();

        // when
        mvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/company/verify-manager").principal(mockPrincipal).content(objectMapper.writeValueAsString(request)).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isBadRequest());
    }

    @Test
    void getActiveOnboarding_success() throws Exception {
        // given
        String taxCode = "validTaxCode";
        String productId = "validProductId";
        InstitutionOnboarding onboarding = new InstitutionOnboarding();
        onboarding.setProductId(productId);
        onboarding.setStatus("ACTIVE");
        onboarding.setCreatedAt(OffsetDateTime.parse("2021-01-01T00:00:00Z"));
        Institution institution = new Institution();
        institution.setOnboarding(List.of(onboarding));

        when(institutionServiceMock.getActiveOnboarding(taxCode, productId, null)).thenReturn(List.of(institution));
        InstitutionOnboardingResource onboardingResource = new InstitutionOnboardingResource();
        onboardingResource.setInstitutionId(institution.getId());

        // when
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/onboarding/active").param("taxCode", taxCode).param("productId", productId).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isOk()).andReturn();

        // then
        List<InstitutionOnboardingResource> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(response.get(0).getInstitutionId(), onboardingResource.getInstitutionId());
    }

    @Test
    void getActiveOnboarding_blankTaxCode() throws Exception {
        // given
        String taxCode = "";

        // when
        mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/onboarding/active").param("taxCode", taxCode).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isBadRequest());
    }

    @Test
    void getActiveOnboarding_blankProductId() throws Exception {
        // given
        String taxCode = "validTaxCode";
        String productId = "";

        // when
        mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/onboarding/active").param("taxCode", taxCode).param("productId", productId).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isInternalServerError());
    }

    @Test
    void getActiveOnboarding_nullTaxCode() throws Exception {
        // given
        String taxCode = null;
        String productId = "validProductId";

        // when
        mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/onboarding/active").param("taxCode", taxCode).param("productId", productId).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isBadRequest());
    }

    @Test
    void getActiveOnboarding_nullProductId() throws Exception {
        // given
        String taxCode = "validTaxCode";
        String productId = null;

        // when
        mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/onboarding/active").param("taxCode", taxCode).param("productId", productId).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isBadRequest());
    }

    @Test
    void verifyAggregatesCsvFailure() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "Hello, World!".getBytes());

        doThrow(new RuntimeException()).when(institutionServiceMock).validateAggregatesCsv(any(MultipartFile.class), anyString());

        // When
        mvc.perform(MockMvcRequestBuilders.multipart(BASE_URL + "/onboarding/aggregation/verification").file(file).param("productId", ProductId.PROD_PAGOPA.name()).contentType(MediaType.MULTIPART_FORM_DATA_VALUE)).andExpect(status().isBadRequest());

        // Then
        verifyNoInteractions(institutionServiceMock);
    }

    @Test
    void verifyRecipientCode() throws Exception {
        // Given
        String recipientCode = "recipientCode";
        String originId = "originId";

        // When
        mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/onboarding/recipient-code/verification").queryParam("recipientCode", recipientCode).queryParam("originId", originId).contentType(APPLICATION_JSON_VALUE)).andExpect(status().isOk());

        // Then
        verify(institutionServiceMock, times(1)).checkRecipientCode(any(), any());
        verifyNoMoreInteractions(institutionServiceMock);
    }

    @Test
    void onboardingUsersPgFromIcAndAde_success() throws Exception {
        // given
        CompanyOnboardingUserDto onboardingData = new CompanyOnboardingUserDto();
        onboardingData.setProductId("productId");
        onboardingData.setTaxCode("taxCode");
        onboardingData.setInstitutionType(it.pagopa.selfcare.onboarding.common.InstitutionType.PG);
        onboardingData.setUsers(List.of(mockInstance(new CompanyUserDto())));


        // when
        mvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/onboarding/users/pg").content(objectMapper.writeValueAsString(onboardingData)).contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE)).andExpect(status().isOk());

        // then
        verify(institutionServiceMock, times(1)).onboardingUsersPgFromIcAndAde(any(OnboardingData.class));
        verifyNoMoreInteractions(institutionServiceMock);
    }

    @Test
    void getOnboardingsInfo() throws Exception {
        // Given
        String taxCode = "taxCode";
        String status = "status";

        // When
        mvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/onboardings").queryParam("taxCode", taxCode).queryParam("status", status).contentType(APPLICATION_JSON_VALUE)).andExpect(status().isOk());

        // Then
        verify(institutionServiceMock, times(1)).getOnboardingWithFilter(any(), any());
        verifyNoMoreInteractions(institutionServiceMock);
    }

}
