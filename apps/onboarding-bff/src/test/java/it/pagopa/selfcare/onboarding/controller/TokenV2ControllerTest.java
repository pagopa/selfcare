package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.security.identity.SecurityIdentity;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.security.AuthorizationService;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.util.PermissionConstants;
import it.pagopa.selfcare.onboarding.controller.request.ReasonForRejectDto;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingRequestResource;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.OnboardingVerify;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.service.UserInstitutionService;
import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@ExtendWith(MockitoExtension.class)
class TokenV2ControllerTest {

    @Mock
    AuthorizationService authorizationService;
    @Mock
    TokenService tokenService;
    @Mock
    UserService userService;
    @Mock
    UserInstitutionService userInstitutionService;
    @Mock
    OnboardingMapper onboardingMapper;
    @Mock
    SecurityIdentity securityIdentity;

    @InjectMocks
    TokenV2Controller tokenV2Controller;

    @BeforeEach
    void setUp() throws Exception {
        Field securityField = TokenV2Controller.class.getDeclaredField("securityIdentity");
        securityField.setAccessible(true);
        securityField.set(tokenV2Controller, securityIdentity);

        Field authField = TokenV2Controller.class.getDeclaredField("authorizationService");
        authField.setAccessible(true);
        authField.set(tokenV2Controller, authorizationService);
    }

    @Test
    void verifyOnboarding_returnsMappedResult() {
        OnboardingData onboardingData = new OnboardingData();
        OnboardingVerify expected = new OnboardingVerify();
        when(tokenService.verifyOnboarding("42")).thenReturn(onboardingData);
        when(onboardingMapper.toOnboardingVerify(onboardingData)).thenReturn(expected);

        OnboardingVerify result = tokenV2Controller.verifyOnboarding("42");

        assertSame(expected, result);
    }

    @Test
    void retrieveOnboardingRequest_returnsMappedResult() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        OnboardingRequestResource expected = new OnboardingRequestResource();
        when(authorizationService.hasPermission(securityIdentity, "42", PermissionConstants.SELC_VIEW_ACCOUNT_PAGE)).thenReturn(true);
        when(tokenService.getOnboardingWithUserInfo("42")).thenReturn(onboardingData);
        when(onboardingMapper.toOnboardingRequestResource(onboardingData)).thenReturn(expected);

        // when
        OnboardingRequestResource result = tokenV2Controller.retrieveOnboardingRequest("42");

        // then
        assertSame(expected, result);
    }

    @Test
    void retrieveOnboardingRequest_throwsWhenUnauthorized() {
        // given
        when(authorizationService.hasPermission(securityIdentity, "42", PermissionConstants.SELC_VIEW_ACCOUNT_PAGE)).thenReturn(false);

        // when / then
        assertThrows(UnauthorizedUserException.class, () -> tokenV2Controller.retrieveOnboardingRequest("42"));
    }

    @Test
    void approveOnboarding_delegatesToService() {
        // given
        when(authorizationService.hasPermission(securityIdentity, "42", PermissionConstants.SELC_MANAGE_ACCOUNT_PAGE)).thenReturn(true);
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");

        // when
        tokenV2Controller.approveOnboarding("42");

        // then
        verify(tokenService).approveOnboarding("42", "test-uid");
    }

    @Test
    void approveOnboarding_throwsWhenUnauthorized() {
        // given
        when(authorizationService.hasPermission(securityIdentity, "42", PermissionConstants.SELC_MANAGE_ACCOUNT_PAGE)).thenReturn(false);

        // when / then
        assertThrows(UnauthorizedUserException.class, () -> tokenV2Controller.approveOnboarding("42"));
    }

    @Test
    void rejectOnboarding_delegatesToService() {
        // given
        ReasonForRejectDto request = new ReasonForRejectDto();
        request.setReason("reason");
        when(authorizationService.hasPermission(securityIdentity, "42", PermissionConstants.SELC_MANAGE_ACCOUNT_PAGE)).thenReturn(true);
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");

        // when
        tokenV2Controller.rejectOnboarding("42", request);

        // then
        verify(tokenService).rejectOnboarding("42", "reason", "test-uid");
    }

    @Test
    void rejectOnboarding_throwsWhenUnauthorized() {
        // given
        ReasonForRejectDto request = new ReasonForRejectDto();
        request.setReason("reason");
        when(authorizationService.hasPermission(securityIdentity, "42", PermissionConstants.SELC_MANAGE_ACCOUNT_PAGE)).thenReturn(false);

        // when / then
        assertThrows(UnauthorizedUserException.class, () -> tokenV2Controller.rejectOnboarding("42", request));
    }

    @Test
    void deleteOnboarding_returnsNoContent() {
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");

        Response response = tokenV2Controller.deleteOnboarding("42");

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(tokenService).rejectOnboarding("42", "REJECTED_BY_USER", "test-uid");
    }

    @Test
    void getContract_returnsBinaryResponse() {
        BinaryData contract = new BinaryData("contract.pdf", "content".getBytes());
        when(tokenService.getContract("42")).thenReturn(contract);

        Response response = tokenV2Controller.getContract("42");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("attachment; filename=contract.pdf", response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void getTemplateAttachment_supportsLegacyAttachmentNameQueryParam() {
        BinaryData contract = new BinaryData("template.pdf", "content".getBytes());
        when(tokenService.getTemplateAttachment("42", "legacy-template.pdf")).thenReturn(contract);

        Response response = tokenV2Controller.getTemplateAttachment("42", null, "legacy-template.pdf");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("attachment; filename=template.pdf", response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));
        verify(tokenService).getTemplateAttachment("42", "legacy-template.pdf");
    }

    @Test
    void getAttachment_throwsWhenAttachmentNameMissing() {
        assertThrows(InvalidRequestException.class, () -> tokenV2Controller.getAttachment("42", null));
    }

    @Test
    void uploadAttachment_throwsWhenAttachmentNameMissing() throws Exception {
        Path tempFile = Files.createTempFile("token-upload-", ".pdf");
        Files.writeString(tempFile, "pdf-content");
        try {
            FileUpload fileUpload = org.mockito.Mockito.mock(FileUpload.class);
            when(fileUpload.fileName()).thenReturn("contract.pdf");
            when(fileUpload.contentType()).thenReturn("application/pdf");
            when(fileUpload.uploadedFile()).thenReturn(tempFile);

            assertThrows(InvalidRequestException.class, () -> tokenV2Controller.uploadAttachment("42", null, null, fileUpload));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void uploadAttachment_supportsLegacyAttachmentNameQueryParam() throws Exception {
        Path tempFile = Files.createTempFile("token-upload-", ".pdf");
        Files.writeString(tempFile, "pdf-content");
        try {
            FileUpload fileUpload = org.mockito.Mockito.mock(FileUpload.class);
            when(fileUpload.fileName()).thenReturn("contract.pdf");
            when(fileUpload.contentType()).thenReturn("application/pdf");
            when(fileUpload.uploadedFile()).thenReturn(tempFile);

            Response response = tokenV2Controller.uploadAttachment("42", null, "legacy-attachment.pdf", fileUpload);

            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            verify(tokenService).uploadAttachment(eq("42"), any(UploadedFile.class), eq("legacy-attachment.pdf"), eq(null), eq(null));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void getAvailableDocuments_returnsMappedResult() {
        // given
        it.pagopa.selfcare.onboarding.client.model.AvailableDocuments source =
                new it.pagopa.selfcare.onboarding.client.model.AvailableDocuments();
        source.setAttachments(java.util.List.of("doc1.pdf"));
        source.setContractFilename("contract.pdf");
        when(authorizationService.hasPermission(securityIdentity, "42", PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS)).thenReturn(true);
        when(tokenService.getAvailableDocuments("42")).thenReturn(source);

        // when
        it.pagopa.selfcare.onboarding.controller.response.AvailableDocumentsResource result =
                tokenV2Controller.getAvailableDocuments("42");

        // then
        assertEquals(java.util.List.of("doc1.pdf"), result.getAttachments());
        assertEquals("contract.pdf", result.getContractFilename());
    }

    @Test
    void getAvailableDocuments_throwsWhenUnauthorized() {
        // given
        when(authorizationService.hasPermission(securityIdentity, "42", PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS)).thenReturn(false);

        // when / then
        assertThrows(UnauthorizedUserException.class, () -> tokenV2Controller.getAvailableDocuments("42"));
    }

    @Test
    void getAttachmentStatus_returnsNoContentWhenFound() {
        // given
        when(tokenService.headAttachment("42", "doc.pdf")).thenReturn(200);

        // when
        Response response = tokenV2Controller.getAttachmentStatus("42", "doc.pdf");

        // then
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void getAttachmentStatus_returnsNotFoundWhenMissing() {
        // given
        when(tokenService.headAttachment("42", "doc.pdf")).thenReturn(404);

        // when
        Response response = tokenV2Controller.getAttachmentStatus("42", "doc.pdf");

        // then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
