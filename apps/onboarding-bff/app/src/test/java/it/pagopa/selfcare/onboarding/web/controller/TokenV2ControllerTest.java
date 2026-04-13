package it.pagopa.selfcare.onboarding.web.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.connector.exceptions.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.InstitutionUpdate;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import it.pagopa.selfcare.onboarding.core.TokenService;
import it.pagopa.selfcare.onboarding.core.UserInstitutionService;
import it.pagopa.selfcare.onboarding.core.UserService;
import it.pagopa.selfcare.onboarding.web.config.WebTestConfig;
import it.pagopa.selfcare.onboarding.web.model.OnboardingRequestResource;
import it.pagopa.selfcare.onboarding.web.model.ReasonForRejectDto;
import it.pagopa.selfcare.onboarding.web.model.mapper.OnboardingResourceMapperImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {TokenV2Controller.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {TokenV2Controller.class, WebTestConfig.class, OnboardingResourceMapperImpl.class})
class TokenV2ControllerTest {

    @Autowired
    protected MockMvc mvc;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserInstitutionService userInstitutionService;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Method under test: {@link TokenV2Controller#complete(String, MultipartFile)}
     */
    @Test
    void shouldCompleteToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "contract", "hello.pdf", MediaType.APPLICATION_PDF_VALUE, "Hello, World!".getBytes());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/v2/tokens/{tokenId}/complete",
                        "42")
                .file(file);
        mvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    /**
     * Method under test: {@link TokenV2Controller#completeOnboardingUsers(String, MultipartFile)}
     */
    @Test
    void shouldCompleteOnboardingUsers() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "contract", "hello.pdf", MediaType.APPLICATION_PDF_VALUE, "Hello, World!".getBytes());

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/v2/tokens/{onboardingId}/complete-onboarding-users", "42")
                .file(file);

        mvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    /**
     * Method under test: {@link TokenV2Controller#verifyOnboarding(String)}
     */
    @Test
    void verifyOnboarding() throws Exception {

        final String onboardingId = UUID.randomUUID().toString();
        when(tokenService.verifyOnboarding(onboardingId))
                .thenReturn(new OnboardingData());

        //when
        mvc.perform(MockMvcRequestBuilders
                        .post("/v2/tokens/{onboardingId}/verify", onboardingId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        //then
        verify(tokenService, times(1))
                .verifyOnboarding(onboardingId);
    }

    /**
     * Method under test: {@link TokenV2Controller#retrieveOnboardingRequest(String)}
     */
    @Test
    void retrieveOnboardingRequest() throws Exception {

        OnboardingData onboardingData = new OnboardingData();
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setTaxCode("taxCode");
        onboardingData.setInstitutionUpdate(institutionUpdate);

        String onboardingId = UUID.randomUUID().toString();
        when(tokenService.getOnboardingWithUserInfo(onboardingId))
                .thenReturn(onboardingData);

        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/v2/tokens/{onboardingId}", onboardingId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        OnboardingRequestResource response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OnboardingRequestResource.class
        );
        //then

        assertEquals(institutionUpdate.getTaxCode(), response.getInstitutionInfo().getFiscalCode());

        verify(tokenService, times(1))
                .getOnboardingWithUserInfo(onboardingId);
    }

    /**
     * Method under test: {@link TokenV2Controller#approveOnboarding(String)}
     */
    @Test
    void approveOnboardingRequest() throws Exception {

        final String onboardingId = UUID.randomUUID().toString();
        doNothing().when(tokenService).approveOnboarding(onboardingId);

        //when
        mvc.perform(MockMvcRequestBuilders
                        .post("/v2/tokens/{onboardingId}/approve", onboardingId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        //then

        verify(tokenService, times(1))
                .approveOnboarding(onboardingId);
    }

    /**
     * Method under test: {@link TokenV2Controller#rejectOnboarding(String, ReasonForRejectDto)}
     */
    @Test
    void rejectOnboardingRequest() throws Exception {

        final String onboardingId = UUID.randomUUID().toString();
        final String reason = "reason";
        ReasonForRejectDto reasonDto = new ReasonForRejectDto();
        reasonDto.setReason(reason);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(reasonDto);

        doNothing().when(tokenService).rejectOnboarding(onboardingId, reason);

        //when
        mvc.perform(MockMvcRequestBuilders
                        .post("/v2/tokens/{onboardingId}/reject", onboardingId)
                        .content(json)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        //then

        verify(tokenService, times(1))
                .rejectOnboarding(onboardingId, reason);
    }

    /**
     * Method under test: {@link TokenV2Controller#deleteOnboarding(String)}
     */
    @Test
    void deleteOnboarding() throws Exception {

        final String onboardingId = UUID.randomUUID().toString();
        final String reason = "REJECTED_BY_USER";

        doNothing().when(tokenService).rejectOnboarding(onboardingId, reason);

        //when
        mvc.perform(MockMvcRequestBuilders
                        .delete("/v2/tokens/{onboardingId}/complete", onboardingId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().is(204))
                .andReturn();
        //then
        verify(tokenService, times(1))
                .rejectOnboarding(onboardingId, reason);
    }

    /**
     * Method under test: {@link TokenV2Controller#getContract(String)}
     */
    @Test
    void getContract() throws Exception {
        String onboardingId = "onboardingId";
        String text = "String";
        byte[] bytes = text.getBytes();
        InputStream is = new ByteArrayInputStream(bytes);
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(tokenService.getContract(onboardingId)).thenReturn(resource);
        Mockito.when(resource.getInputStream()).thenReturn(is);

        //when
        mvc.perform(MockMvcRequestBuilders
                        .get("/v2/tokens/{onboardingId}/contract", onboardingId)
                        .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        //then
        verify(tokenService, times(1))
                .getContract(onboardingId);
    }

    /**
     * Method under test: {@link TokenV2Controller#getTemplateAttachment(String, String)}
     */
    @Test
    void getTemplateAttachment() throws Exception {
        final String onboardingId = "onboardingId";
        final String text = "String";
        final String filename = "filename";
        byte[] bytes = text.getBytes();
        InputStream is = new ByteArrayInputStream(bytes);
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(tokenService.getTemplateAttachment(onboardingId, filename)).thenReturn(resource);
        Mockito.when(resource.getInputStream()).thenReturn(is);

        //when
        mvc.perform(MockMvcRequestBuilders
                        .get("/v2/tokens/{onboardingId}/template-attachment?name={name}", onboardingId, filename)
                        .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        //then
        verify(tokenService, times(1))
                .getTemplateAttachment(onboardingId, filename);
    }


    @Test
    void headAttachmentTest() throws Exception {
        // given
        final String onboardingId = "onboardingId";
        final String filename = "filename";

        Mockito.when(tokenService.headAttachment(onboardingId, filename)).thenReturn(HttpStatusCode.valueOf(204));

        //when
        mvc.perform(MockMvcRequestBuilders
                        .head("/v2/tokens/{onboardingId}/attachment/status", onboardingId)
                        .queryParam("name", filename))
                .andExpect(status().isNoContent())
                .andReturn();

        //then
        verify(tokenService, times(1))
                .headAttachment(eq("onboardingId"), eq("filename"));
    }

    @Test
    void headAttachmentTest_shouldReturnNotFound() throws Exception {
        // given
        final String onboardingId = "onboardingId";
        final String filename = "filename";

        Mockito.when(tokenService.headAttachment(onboardingId, filename)).thenReturn(HttpStatusCode.valueOf(404));

        //when
        mvc.perform(MockMvcRequestBuilders
                        .head("/v2/tokens/{onboardingId}/attachment/status", onboardingId)
                        .queryParam("name", filename))
                .andExpect(status().isNotFound())
                .andReturn();

        //then
        verify(tokenService, times(1))
                .headAttachment(eq("onboardingId"), eq("filename"));
    }

    /**
     * Method under test: {@link TokenV2Controller#uploadAttachment(String, String, MultipartFile)}
     */
    @Test
    void uploadAttachment() throws Exception {
        final String onboardingId = "onboardingId";
        final String filename = "filename";

        MockMultipartFile file = new MockMultipartFile(
                "attachment",         
                "hello.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "Hello, World!".getBytes()
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/v2/tokens/{onboardingId}/attachment", onboardingId)
                .file(file)
                .queryParam("name", filename);

        mvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        verify(tokenService, times(1))
                .uploadAttachment(onboardingId, file, filename);
    }


    /**
     * Method under test: {@link TokenV2Controller#getAggregatesCsv(String, String, java.security.Principal)}
     */
    @Test
    void getAggregatesCsv_Case1() throws Exception {
        //given
        String onboardingId = "onboardingId";
        String productId = "productId";
        String text = "String";

        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example")
                .fiscalCode("fiscalCode")
                .build();
        Mockito.when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);

        OnboardingData onboardingData = dummyOnboardingData();
        String uid = selfCareUser.getId();

        Mockito.when(tokenService.getOnboardingWithUserInfo(onboardingId))
            .thenReturn(onboardingData);

        Mockito.when(userInstitutionService.verifyAllowedUserInstitution(onboardingData.getInstitutionUpdate().getId(), productId, uid))
            .thenReturn(true);
        Mockito.when(tokenService.verifyAllowedUserByRole(onboardingId, uid))
            .thenReturn(false);
        Mockito.when(userService.isAllowedUserByUid(uid))
            .thenReturn(false);

        byte[] bytes= text.getBytes();
        InputStream is = new ByteArrayInputStream(bytes);
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(tokenService.getAggregatesCsv(onboardingId, productId)).thenReturn(resource);
        Mockito.when(resource.getInputStream()).thenReturn(is);

        // when
        mvc.perform(
                MockMvcRequestBuilders.get(
                        "/v2/tokens/{onboardingId}/products/{productId}/aggregates-csv",
                        onboardingId,
                        productId)
                    .principal(mockPrincipal)
                    .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        //then
        verify(tokenService, times(1))
            .getOnboardingWithUserInfo(onboardingId);
        verify(tokenService, times(1))
            .getAggregatesCsv(onboardingId, productId);
        verify(tokenService, times(0))
            .verifyAllowedUserByRole(onboardingId, uid);
        verify(userService, times(0))
            .isAllowedUserByUid(uid);
        verify(userInstitutionService, times(1))
            .verifyAllowedUserInstitution(onboardingData.getInstitutionUpdate().getId(), productId, uid);
        verifyNoMoreInteractions(tokenService);
        verifyNoMoreInteractions(userInstitutionService);
        verifyNoMoreInteractions(userService);
    }

    /**
     * Method under test: {@link TokenV2Controller#getAggregatesCsv(String, String, java.security.Principal)}
     */
    @Test
    void getAggregatesCsv_Case2() throws Exception {
        //given
        String onboardingId = "onboardingId";
        String productId = "productId";
        String text = "String";

        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example")
            .fiscalCode("fiscalCode")
            .build();
        Mockito.when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);

        String uid = selfCareUser.getId();
        OnboardingData onboardingData = dummyOnboardingData();

        Mockito.when(tokenService.getOnboardingWithUserInfo(onboardingId))
            .thenReturn(onboardingData);

        Mockito.when(userInstitutionService.verifyAllowedUserInstitution(onboardingData.getInstitutionUpdate().getId(), productId, uid))
            .thenReturn(false);
        Mockito.when(tokenService.verifyAllowedUserByRole(onboardingId, uid))
        .thenReturn(true);
        Mockito.when(userService.isAllowedUserByUid(uid))
            .thenReturn(false);

        byte[] bytes = text.getBytes();
        InputStream is = new ByteArrayInputStream(bytes);
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(tokenService.getAggregatesCsv(onboardingId, productId)).thenReturn(resource);
        Mockito.when(resource.getInputStream()).thenReturn(is);

        // when
        mvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/v2/tokens/{onboardingId}/products/{productId}/aggregates-csv",
                                        onboardingId,
                                        productId)
                                .principal(mockPrincipal)
                                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        //then
        verify(tokenService, times(1))
            .getOnboardingWithUserInfo(onboardingId);
        verify(tokenService, times(1))
                .getAggregatesCsv(onboardingId, productId);
        verify(tokenService, times(1))
            .verifyAllowedUserByRole(onboardingId, uid);
        verify(userInstitutionService, times(1))
            .verifyAllowedUserInstitution(onboardingData.getInstitutionUpdate().getId(), productId, uid);
        verify(userService, times(0))
            .isAllowedUserByUid(uid);

        verifyNoMoreInteractions(tokenService);
        verifyNoMoreInteractions(userInstitutionService);
        verifyNoMoreInteractions(userService);
    }

    /**
     * Method under test: {@link TokenV2Controller#getAggregatesCsv(String, String, java.security.Principal)}
     */
    @Test
    void getAggregatesCsv_Case3() throws Exception {
        //given
        String onboardingId = "onboardingId";
        String productId = "productId";
        String text = "String";

        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example")
                .fiscalCode("fiscalCode")
                .build();
        Mockito.when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);

        String uid = selfCareUser.getId();
        OnboardingData onboardingData = dummyOnboardingData();

        Mockito.when(tokenService.getOnboardingWithUserInfo(onboardingId))
            .thenReturn(onboardingData);

        Mockito.when(userInstitutionService.verifyAllowedUserInstitution(onboardingData.getInstitutionUpdate().getId(), productId, uid))
            .thenReturn(false);
        Mockito.when(tokenService.verifyAllowedUserByRole(onboardingId, uid))
            .thenReturn(false);
        Mockito.when(userService.isAllowedUserByUid(uid))
            .thenReturn(true);

        byte[] bytes = text.getBytes();
        InputStream is = new ByteArrayInputStream(bytes);
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(tokenService.getAggregatesCsv(onboardingId, productId)).thenReturn(resource);
        Mockito.when(resource.getInputStream()).thenReturn(is);

        // when
        mvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/v2/tokens/{onboardingId}/products/{productId}/aggregates-csv",
                                        onboardingId,
                                        productId)
                                .principal(mockPrincipal)
                                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        //then
        verify(tokenService, times(1))
            .getOnboardingWithUserInfo(onboardingId);
        verify(tokenService, times(1))
            .getAggregatesCsv(onboardingId, productId);
        verify(tokenService, times(1))
            .verifyAllowedUserByRole(onboardingId, uid);
        verify(userInstitutionService, times(1))
            .verifyAllowedUserInstitution(onboardingData.getInstitutionUpdate().getId(), productId, uid);
        verify(userService, times(1))
            .isAllowedUserByUid(uid);

        verifyNoMoreInteractions(tokenService);
        verifyNoMoreInteractions(userInstitutionService);
        verifyNoMoreInteractions(userService);
    }


    /**
     * Method under test: {@link TokenV2Controller#getAggregatesCsv(String, String, java.security.Principal)}
     */
    @Test
    void getAggregatesCsv_CaseKO() throws Exception {
        //given
        String onboardingId = "onboardingId";
        String productId = "productId";

        JwtAuthenticationToken mockPrincipal = Mockito.mock(JwtAuthenticationToken.class);
        SelfCareUser selfCareUser = SelfCareUser.builder("example")
                .fiscalCode("fiscalCode")
                .build();
        Mockito.when(mockPrincipal.getPrincipal()).thenReturn(selfCareUser);

        String uid = selfCareUser.getId();
        OnboardingData onboardingData = dummyOnboardingData();

        Mockito.when(tokenService.getOnboardingWithUserInfo(onboardingId))
            .thenReturn(onboardingData);

        Mockito.when(userInstitutionService.verifyAllowedUserInstitution(onboardingData.getInstitutionUpdate().getId(), productId, uid))
            .thenReturn(false);
        Mockito.when(tokenService.verifyAllowedUserByRole(onboardingId, uid))
            .thenReturn(false);
        Mockito.when(userService.isAllowedUserByUid(uid))
            .thenReturn(false);

        // when
        mvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/v2/tokens/{onboardingId}/products/{productId}/aggregates-csv",
                                        onboardingId,
                                        productId)
                                .principal(mockPrincipal)
                                .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UnauthorizedUserException))
                .andExpect(status().isForbidden())
                .andReturn();

        //then
        verify(tokenService, times(1))
            .getOnboardingWithUserInfo(onboardingId);

        verify(tokenService, times(1))
            .verifyAllowedUserByRole(onboardingId, uid);
        verifyNoMoreInteractions(tokenService);
    }

  @NotNull
  private static OnboardingData dummyOnboardingData() {
    OnboardingData onboardingData = new OnboardingData();
    onboardingData.setId("onboardingId");
    onboardingData.setProductId("productId");
    InstitutionUpdate institutionUpdate = new InstitutionUpdate();
    institutionUpdate.setId("TEST-1234");
    onboardingData.setInstitutionUpdate(institutionUpdate);
    onboardingData.setStatus(String.valueOf(OnboardingStatus.COMPLETED));
    User user = new User();
    user.setId("example");
    onboardingData.setUsers(List.of(user));

    return onboardingData;
  }
}
