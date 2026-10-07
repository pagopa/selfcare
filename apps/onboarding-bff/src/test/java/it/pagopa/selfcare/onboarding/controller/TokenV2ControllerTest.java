package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.security.identity.SecurityIdentity;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.controller.request.ReasonForRejectDto;
import it.pagopa.selfcare.onboarding.controller.response.AvailableDocumentsResource;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingRequestResource;
import it.pagopa.selfcare.onboarding.exception.AccessDeniedException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.OnboardingVerify;
import it.pagopa.selfcare.onboarding.security.AuthorizationService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.util.PermissionConstants;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenV2ControllerTest {

    private static final String VIEW_PAGE = PermissionConstants.SELC_VIEW_ACCOUNT_PAGE;
    private static final String VIEW_DOCUMENTS = PermissionConstants.SELC_VIEW_ACCOUNT_DOCUMENTS;
    private static final String MANAGE_PAGE = PermissionConstants.SELC_MANAGE_ACCOUNT_PAGE;
    private static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    @Mock
    AuthorizationService authorizationService;
    @Mock
    TokenService tokenService;
    @Mock
    OnboardingMapper onboardingMapper;
    @Mock
    SecurityIdentity securityIdentity;

    TokenV2Controller controller;

    private static UriInfo downloadQuery(String type, String name) {
        var uriInfo = mock(UriInfo.class);
        var query = new MultivaluedHashMap<String, String>();
        if (type != null) {
            query.putSingle("type", type);
        }
        if (name != null) {
            query.putSingle("name", name);
        }
        when(uriInfo.getQueryParameters()).thenReturn(query);
        return uriInfo;
    }

    @BeforeEach
    void setUp() {
        controller = new TokenV2Controller(tokenService, onboardingMapper);
        controller.securityIdentity = securityIdentity;
        controller.authorizationService = authorizationService;
    }

    @Test
    void verifyOnboarding_returnsMappedResult() {
        OnboardingData onboardingData = new OnboardingData();
        OnboardingVerify expected = new OnboardingVerify();
        when(tokenService.verifyOnboarding("42")).thenReturn(onboardingData);
        when(onboardingMapper.toOnboardingVerify(onboardingData)).thenReturn(expected);

        assertSame(expected, controller.verifyOnboarding("42"));
    }

    @Test
    void retrieveOnboardingRequest_checksViewAccountPageThenMapsTheResult() {
        OnboardingData onboardingData = new OnboardingData();
        OnboardingRequestResource expected = new OnboardingRequestResource();
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_PAGE)).thenReturn(true);
        when(tokenService.getOnboardingWithUserInfo("42")).thenReturn(onboardingData);
        when(onboardingMapper.toOnboardingRequestResource(onboardingData)).thenReturn(expected);

        assertSame(expected, controller.retrieveOnboardingRequest("42"));
    }

    @Test
    void retrieveOnboardingRequest_deniedIsAccessDeniedWithoutReadingTheOnboarding() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_PAGE)).thenReturn(false);

        AccessDeniedException e = assertThrows(AccessDeniedException.class, () -> controller.retrieveOnboardingRequest("42"));

        assertEquals("Access Denied", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void approveOnboarding_requiresManageAccountPageAndForwardsTheCallerUid() {
        when(authorizationService.hasPermission(securityIdentity, "42", MANAGE_PAGE)).thenReturn(true);
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");

        Response response = controller.approveOnboarding("42");

        assertEquals(200, response.getStatus());
        verify(tokenService).approveOnboarding("42", "test-uid");
    }

    @Test
    void approveOnboarding_deniedIsAccessDenied() {
        when(authorizationService.hasPermission(securityIdentity, "42", MANAGE_PAGE)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> controller.approveOnboarding("42"));
        verifyNoInteractions(tokenService);
    }

    @Test
    void rejectOnboarding_requiresManageAccountPageAndForwardsTheReason() {
        ReasonForRejectDto request = new ReasonForRejectDto();
        request.setReason("reason");
        when(authorizationService.hasPermission(securityIdentity, "42", MANAGE_PAGE)).thenReturn(true);
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");

        Response response = controller.rejectOnboarding("42", request);

        assertEquals(200, response.getStatus());
        verify(tokenService).rejectOnboarding("42", "reason", "test-uid");
    }

    @Test
    void rejectOnboarding_deniedIsAccessDenied() {
        ReasonForRejectDto request = new ReasonForRejectDto();
        request.setReason("reason");
        when(authorizationService.hasPermission(securityIdentity, "42", MANAGE_PAGE)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> controller.rejectOnboarding("42", request));
        verifyNoInteractions(tokenService);
    }

    @Test
    void rejectOnboarding_missingBodyIsABadRequestBeforeAnyPermissionCheck() {
        assertThrows(InvalidRequestException.class, () -> controller.rejectOnboarding("42", null));
        verifyNoInteractions(authorizationService, tokenService);
    }

    @Test
    void deleteOnboarding_rejectsAsUserWithoutIamCheck() {
        when(securityIdentity.getAttribute("uid")).thenReturn("test-uid");

        Response response = controller.deleteOnboarding("42");

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(tokenService).rejectOnboarding("42", "REJECTED_BY_USER", "test-uid");
        verifyNoInteractions(authorizationService);
    }

    @Test
    void getContract_requiresViewAccountDocumentsAndExposesTheFileName() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(true);
        when(tokenService.getContract("42")).thenReturn(new BinaryData("contract.pdf", "content".getBytes()));

        Response response = controller.getContract("42");

        assertBinary(response, "contract.pdf", "content".getBytes());
    }

    @Test
    void getContract_deniedIsAccessDenied() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> controller.getContract("42"));
        verifyNoInteractions(tokenService);
    }

    @Test
    void getTemplateAttachment_requiresAttachmentNameAndSkipsIam() {
        assertThrows(InvalidRequestException.class, () -> controller.getTemplateAttachment("42", null));
        verifyNoInteractions(tokenService, authorizationService);
    }

    @Test
    void getTemplateAttachment_returnsTheBinary() {
        when(tokenService.getTemplateAttachment("42", "template.pdf"))
                .thenReturn(new BinaryData("template.pdf", "content".getBytes()));

        Response response = controller.getTemplateAttachment("42", "template.pdf");

        assertBinary(response, "template.pdf", "content".getBytes());
        verifyNoInteractions(authorizationService);
    }

    @Test
    void getAttachment_requiresNameAndSkipsIam() {
        assertThrows(InvalidRequestException.class, () -> controller.getAttachment("42", null));
        verifyNoInteractions(tokenService, authorizationService);
    }

    @Test
    void getAttachment_returnsTheBinaryWithoutIam() {
        when(tokenService.getAttachment("42", "doc.pdf")).thenReturn(new BinaryData("doc.pdf", new byte[] {1, 2}));

        Response response = controller.getAttachment("42", "doc.pdf");

        assertBinary(response, "doc.pdf", new byte[] {1, 2});
        verifyNoInteractions(authorizationService);
    }

    @Test
    void getAvailableDocuments_requiresViewAccountDocuments() {
        AvailableDocuments source = new AvailableDocuments();
        source.setAttachments(List.of("doc1.pdf"));
        source.setContractFilename("contract.pdf");
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(true);
        when(tokenService.getAvailableDocuments("42")).thenReturn(source);

        AvailableDocumentsResource result = controller.getAvailableDocuments("42");

        assertEquals(List.of("doc1.pdf"), result.getAttachments());
        assertEquals("contract.pdf", result.getContractFilename());
    }

    @Test
    void getAvailableDocuments_deniedIsAccessDenied() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> controller.getAvailableDocuments("42"));
        verifyNoInteractions(tokenService);
    }

    @Test
    void getAggregatesCsv_requiresViewAccountDocuments() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(true);
        when(tokenService.getAggregatesCsv("42", "prod-io")).thenReturn(new BinaryData("agg.csv", "a;b".getBytes()));

        assertBinary(controller.getAggregatesCsv("42", "prod-io"), "agg.csv", "a;b".getBytes());
    }

    @Test
    void getAggregatesCsv_deniedIsAccessDenied() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> controller.getAggregatesCsv("42", "prod-io"));
        verifyNoInteractions(tokenService);
    }

    @Test
    void downloadDocument_signedContract() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(true);
        when(tokenService.getContractSigned("42")).thenReturn(new BinaryData("signed.pdf", "x".getBytes()));

        Response response = controller.downloadDocument("42", downloadQuery("CONTRACT_SIGNED", null));

        assertBinary(response, "signed.pdf", "x".getBytes());
        verify(tokenService, never()).getAttachment(any(), any());
    }

    @Test
    void downloadDocument_attachment() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(true);
        when(tokenService.getAttachment("42", "doc.pdf")).thenReturn(new BinaryData("doc.pdf", "x".getBytes()));

        assertBinary(controller.downloadDocument("42", downloadQuery("ATTACHMENT", "doc.pdf")), "doc.pdf", "x".getBytes());
    }

    @Test
    void downloadDocument_attachmentWithoutNameIsABadRequestAfterTheIamCheck() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(true);

        InvalidRequestException missing = assertThrows(InvalidRequestException.class,
                () -> controller.downloadDocument("42", downloadQuery("ATTACHMENT", null)));
        InvalidRequestException blank = assertThrows(InvalidRequestException.class,
                () -> controller.downloadDocument("42", downloadQuery("ATTACHMENT", "  ")));

        assertEquals("Query parameter 'name' is required when type=ATTACHMENT", missing.getMessage());
        assertEquals(missing.getMessage(), blank.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void downloadDocument_missingOrUnknownTypeIsABadRequestWithoutAnyDownstreamCall() {
        assertThrows(InvalidRequestException.class, () -> controller.downloadDocument("42", downloadQuery(null, null)));
        assertThrows(InvalidRequestException.class, () -> controller.downloadDocument("42", downloadQuery("OTHER", "doc.pdf")));
        verifyNoInteractions(authorizationService, tokenService);
    }

    @Test
    void downloadDocument_deniedIsAccessDenied() {
        when(authorizationService.hasPermission(securityIdentity, "42", VIEW_DOCUMENTS)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> controller.downloadDocument("42", downloadQuery("CONTRACT_SIGNED", null)));
        verifyNoInteractions(tokenService);
    }

    @Test
    void attachmentStatus_isNoContentOn2xxAndNotFoundOtherwise() {
        when(tokenService.headAttachment("42", "ok.pdf")).thenReturn(200);
        when(tokenService.headAttachment("42", "gone.pdf")).thenReturn(404);
        when(tokenService.headAttachment("42", "err.pdf")).thenReturn(500);

        assertEquals(204, controller.getAttachmentStatus("42", "ok.pdf").getStatus());
        assertEquals(204, controller.headAttachment("42", "ok.pdf").getStatus());
        assertEquals(404, controller.getAttachmentStatus("42", "gone.pdf").getStatus());
        assertEquals(404, controller.headAttachment("42", "err.pdf").getStatus());
        verifyNoInteractions(authorizationService);
    }

    @Test
    void attachmentStatus_requiresName() {
        assertThrows(InvalidRequestException.class, () -> controller.getAttachmentStatus("42", null));
        assertThrows(InvalidRequestException.class, () -> controller.headAttachment("42", null));
        verifyNoInteractions(tokenService);
    }

    @Test
    void uploadAttachment_requiresAttachmentNameBeforeAnyWork() {
        FileUpload upload = mock(FileUpload.class);

        assertThrows(InvalidRequestException.class,
                () -> controller.uploadAttachment("42", null, null, null, upload, "tenant"));
        verifyNoInteractions(tokenService);
    }

    @Test
    void uploadAttachment_requiresTheFilePart() {
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> controller.uploadAttachment("42", "att", null, null, null, "tenant"));

        assertEquals("Required part 'attachment' is not present.", e.getMessage());
        verifyNoInteractions(tokenService);
    }

    @Test
    void uploadAttachment_requiresATenant() throws Exception {
        Path tempFile = pdf();
        try {
            FileUpload upload = upload(tempFile, "contract.pdf", "application/pdf");

            InvalidRequestException blank = assertThrows(InvalidRequestException.class,
                    () -> controller.uploadAttachment("42", "att", null, null, upload, " "));
            InvalidRequestException missing = assertThrows(InvalidRequestException.class,
                    () -> controller.uploadAttachment("42", "att", null, null, upload, null));

            assertEquals("Tenant context is required", blank.getMessage());
            assertEquals("Tenant context is required", missing.getMessage());
            verifyNoInteractions(tokenService);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void uploadAttachment_rejectsFormatsOtherThanPdfAndP7m() throws Exception {
        Path tempFile = Files.createTempFile("token-upload-", ".txt");
        Files.writeString(tempFile, "text");
        try {
            FileUpload upload = upload(tempFile, "contract.txt", "text/plain");

            InvalidRequestException e = assertThrows(InvalidRequestException.class,
                    () -> controller.uploadAttachment("42", "att", null, null, upload, "tenant"));

            assertEquals("Formato file non supportato. Ammessi: [.pdf, .p7m]", e.getMessage());
            verifyNoInteractions(tokenService);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void uploadAttachment_forwardsTenantNameAndMultipartFields() throws Exception {
        Path tempFile = pdf();
        try {
            FileUpload upload = upload(tempFile, "contract.pdf", "application/pdf");

            Response response = controller.uploadAttachment("42", "att", "att-id", "att description", upload, "tenant-1");

            assertEquals(204, response.getStatus());
            verify(tokenService).uploadAttachment(eq("tenant-1"), eq("42"), any(UploadedFile.class),
                    eq("att"), eq("att-id"), eq("att description"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void complete_acceptsPdfAndP7mOnly() throws Exception {
        Path pdf = pdf();
        Path p7m = Files.createTempFile("token-upload-", ".p7m");
        Files.writeString(p7m, "signed");
        Path txt = Files.createTempFile("token-upload-", ".txt");
        Files.writeString(txt, "text");
        try {
            assertEquals(204, controller.complete("42", upload(pdf, "c.pdf", "application/pdf")).getStatus());
            assertEquals(204, controller.complete("42", upload(p7m, "c.pdf.p7m", "application/pkcs7-mime")).getStatus());
            FileUpload rejected = upload(txt, "c.txt", "text/plain");
            InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> controller.complete("42", rejected));

            assertEquals("Formato file non supportato. Ammessi: [.pdf, .p7m]", e.getMessage());
            verify(tokenService, times(2)).completeTokenV2(eq("42"), any(UploadedFile.class));
            assertThrows(InvalidRequestException.class, () -> controller.complete("42", null));
        } finally {
            Files.deleteIfExists(pdf);
            Files.deleteIfExists(p7m);
            Files.deleteIfExists(txt);
        }
    }

    @Test
    void completeOnboardingUsers_acceptsPdfAndP7mOnly() throws Exception {
        Path pdf = pdf();
        Path txt = Files.createTempFile("token-upload-", ".txt");
        Files.writeString(txt, "text");
        try {
            assertEquals(204, controller.completeOnboardingUsers("42", upload(pdf, "c.pdf", "application/pdf")).getStatus());
            FileUpload rejected = upload(txt, "c.txt", "text/plain");
            assertThrows(InvalidRequestException.class, () -> controller.completeOnboardingUsers("42", rejected));

            verify(tokenService).completeOnboardingUsers(eq("42"), any(UploadedFile.class));
        } finally {
            Files.deleteIfExists(pdf);
            Files.deleteIfExists(txt);
        }
    }

    private static void assertBinary(Response response, String fileName, byte[] content) {
        assertEquals(200, response.getStatus());
        assertEquals("attachment; filename=" + fileName, response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(HttpHeaders.CONTENT_DISPOSITION, response.getHeaderString(EXPOSE_HEADERS));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE, response.getMediaType());
        assertArrayEquals(content, (byte[]) response.getEntity());
    }

    private static Path pdf() throws Exception {
        Path tempFile = Files.createTempFile("token-upload-", ".pdf");
        Files.writeString(tempFile, "pdf-content");
        return tempFile;
    }

    private static FileUpload upload(Path tempFile, String fileName, String contentType) {
        FileUpload upload = mock(FileUpload.class);
        when(upload.fileName()).thenReturn(fileName);
        when(upload.contentType()).thenReturn(contentType);
        when(upload.uploadedFile()).thenReturn(tempFile);
        return upload;
    }
}
