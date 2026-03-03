package it.pagopa.selfcare.onboarding.service;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.TokenMapper;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.service.impl.TokenServiceDefault;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class TokenServiceDefaultTest {

    private static final String onboardingId = "onboardingId";
    @Inject
    TokenServiceDefault tokenService;

    @InjectMock
    AzureBlobClient azureBlobClient;

    @InjectMock
    SignatureService signatureService;

    @InjectMock
    PadesSignService padesSignService;

    @InjectMock
    ProductService productService;

    @InjectMock
    TokenMapper tokenMapper;

    @Test
    void getToken() {
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.list()).thenReturn(Uni.createFrom().item(List.of(new Token())));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId", onboardingId)).thenReturn(queryPage);

        tokenService
                .getToken(onboardingId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();
    }

    @Test
    void retrieveContractNotSigned() {
        Token token = new Token();
        token.setContractFilename("fileName");
        token.setType(TokenType.INSTITUTION);
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);

        PanacheMock.mock(Token.class);
        when(Token.findById(onboardingId)).thenReturn(Uni.createFrom().item(token));

        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

        UniAssertSubscriber<RestResponse<File>> subscriber =
                tokenService
                        .retrieveContract(onboardingId, false)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        RestResponse<File> actual = subscriber.awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
    }

    @Test
    void retrieveContractSignedTest() {
        // given
        Token token = new Token();
        token.setContractSigned("parties/docs/test-path/NomeDocumentoProva.pdf");
        token.setType(TokenType.INSTITUTION);
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);

        PanacheMock.mock(Token.class);
        when(Token.findById(onboardingId)).thenReturn(Uni.createFrom().item(token));

        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

        // when
        UniAssertSubscriber<RestResponse<File>> subscriber =
                tokenService
                        .retrieveContract(onboardingId, true)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        // then
        RestResponse<File> actual = subscriber.awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
    }

    @Test
    void retrieveContractSignedKOTest() {
        // given
        Token token = new Token();
        token.setContractSigned("parties/docs/test-path/NomeDocumentoProva.p7m");
        token.setType(TokenType.INSTITUTION);
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);

        PanacheMock.mock(Token.class);
        when(Token.findById(onboardingId)).thenReturn(Uni.createFrom().item(token));

        ClassLoader classLoader = getClass().getClassLoader();
        String resourcePath =
                Objects.requireNonNull(classLoader.getResource("documents/contract_error.p7m")).getPath();

        when(azureBlobClient.retrieveFile(anyString())).thenReturn(new File(resourcePath));

        // when
        UniAssertSubscriber<RestResponse<File>> subscriber =
                tokenService
                        .retrieveSignedFile(onboardingId)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        // then
        RestResponse<File> actual = subscriber.awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(RestResponse.Status.NOT_FOUND.getStatusCode(), actual.getStatus());
    }

    @Test
    void retrieveContractSignedPdfOKTest() {
        // given
        Token token = new Token();
        token.setContractSigned("parties/docs/test-path/NomeDocumentoProva.pdf");
        token.setType(TokenType.INSTITUTION);
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);

        PanacheMock.mock(Token.class);
        when(Token.findById(onboardingId)).thenReturn(Uni.createFrom().item(token));

        ClassLoader classLoader = getClass().getClassLoader();
        String resourcePath =
                Objects.requireNonNull(classLoader.getResource("documents/test.pdf")).getPath();

        when(azureBlobClient.retrieveFile(anyString())).thenReturn(new File(resourcePath));

        // when
        UniAssertSubscriber<RestResponse<File>> subscriber =
                tokenService
                        .retrieveSignedFile(onboardingId)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        // then
        RestResponse<File> actual = subscriber.awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
    }

    @Test
    void retrieveContractSignedP7mOKTest() {
        // given
        Token token = new Token();
        token.setContractSigned("parties/docs/test-path/NomeDocumentoProva.p7m");
        token.setType(TokenType.INSTITUTION);

        PanacheMock.mock(Token.class);
        when(Token.findById(onboardingId)).thenReturn(Uni.createFrom().item(token));

        ClassLoader classLoader = getClass().getClassLoader();
        String resourcePath =
                Objects.requireNonNull(classLoader.getResource("documents/test.pdf.p7m")).getPath();
        when(azureBlobClient.retrieveFile(anyString())).thenReturn(new File(resourcePath));

        String resourceExtractedPath =
                Objects.requireNonNull(classLoader.getResource("documents/test.pdf")).getPath();
        when(signatureService.extractFile(any())).thenReturn(new File(resourceExtractedPath));

        // when
        UniAssertSubscriber<RestResponse<File>> subscriber =
                tokenService
                        .retrieveSignedFile(onboardingId)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        // then
        RestResponse<File> actual = subscriber.awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
    }

    @Test
    void retrieveAttachment_Template_onboardingNotFound() {
        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(anyString())).thenReturn(Uni.createFrom().nullItem());

        UniAssertSubscriber<RestResponse<File>> subscriber =
                tokenService
                        .retrieveTemplateAttachment("id", "file")
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure().assertFailedWith(ResourceNotFoundException.class);
    }

    @Test
    void retrieveTemplateAttachmentGeneratedSuccess() {
        final String onboardingId = "onboardingId";
        final String filename = "filename.pdf";

        Token token = new Token();
        token.setContractFilename(filename);
        token.setContractSigned(filename);
        token.setName(filename);

        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().item(token));

        PanacheMock.mock(Token.class);
        when(Token.find(
                "onboardingId = ?1 and type = ?2 and name = ?3",
                onboardingId,
                ATTACHMENT.name(),
                filename))
                .thenReturn(queryPage);

        File file = new File("filename.pdf");
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(file);

        UniAssertSubscriber<RestResponse<File>> subscriber =
                tokenService
                        .retrieveAttachment(onboardingId, filename)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        RestResponse<File> response = subscriber.awaitItem().getItem();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(file, response.getEntity());
        verify(azureBlobClient).getFileAsPdf(anyString());
    }

    @Test
    void retrieveTemplateAttachmentGeneratedFalseSuccess() {
        final String onboardingId = "onboardingId";
        final String filename = "filename.pdf";
        final String productId = "productId";
        final String institutionType = "PA";

        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);
        onboarding.setProductId(productId);

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);

        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboardingId)).thenReturn(Uni.createFrom().item(onboarding));

        AttachmentTemplate attachment = new AttachmentTemplate();
        attachment.setName(filename);
        attachment.setGenerated(false);
        attachment.setTemplatePath("templatePath");
        Product product = createProductWithAttachment(institutionType, attachment);

        when(productService.getProductIsValid(productId)).thenReturn(product);

        File file = new File("test.pdf");
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(file);

        UniAssertSubscriber<RestResponse<File>> subscriber =
                tokenService
                        .retrieveTemplateAttachment(onboardingId, filename)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        RestResponse<File> response = subscriber.awaitItem().getItem();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(file, response.getEntity());

        verify(productService).getProductIsValid(productId);
        verify(azureBlobClient).getFileAsPdf(anyString());
    }

    @Test
    void uploadAttachmentErrorDigest() {

        final String onboardingId = "onboardingId";
        final String filename = "filename.pdf";
        final String productId = "productId";

        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);
        onboarding.setProductId(productId);

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);

        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboardingId)).thenReturn(Uni.createFrom().item(onboarding));

        AttachmentTemplate attachment = new AttachmentTemplate();
        attachment.setName(filename);
        attachment.setTemplatePath("template.pdf");
        attachment.setGenerated(true);

        Product product = createProductWithAttachment("PA", attachment);
        when(productService.getProductIsValid(productId)).thenReturn(product);

        File uploadedFile = new File("src/test/resources/test.pdf");

        File originalFile = new File("src/test/resources/original.pdf");

        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(originalFile);

        FormItem formItem = FormItem.builder().file(uploadedFile).fileName(filename).build();

        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboardingId))
                .thenReturn(Uni.createFrom().item(onboarding));

        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().nullItem());

        DSSDocument document = new FileDocument(uploadedFile);
        DSSDocument document2 = new FileDocument(originalFile);
        when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn(document).thenReturn(document2);

        when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("fake-digest").thenReturn("fake-digest2");

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3",
                onboardingId,
                ATTACHMENT.name(),
                filename
        )).thenReturn(queryPage);

        when(azureBlobClient.getProperties(anyString())).thenReturn(null);

        UniAssertSubscriber<Void> subscriber =
                tokenService
                        .uploadAttachment(onboardingId, formItem, filename)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();

        subscriber.assertFailedWith(InvalidRequestException.class);

        verify(productService).getProductIsValid(productId);
        verify(azureBlobClient).getFileAsPdf(anyString());
    }

    @Test
    @RunOnVertxContext
    void uploadAttachmentSuccess(UniAsserter asserter) {

        final String onboardingId = "onboardingId";
        final String filename = "filename.pdf";
        final String productId = "productId";

        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);
        onboarding.setProductId(productId);

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);

        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboardingId))
                .thenReturn(Uni.createFrom().item(onboarding));

        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().nullItem());

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3",
                onboardingId,
                ATTACHMENT.name(),
                filename
        )).thenReturn(queryPage);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(
                () -> when(Onboarding.findById(anyString())).thenReturn(Uni.createFrom().item(onboarding)));

        AttachmentTemplate attachment = new AttachmentTemplate();
        attachment.setName(filename);
        attachment.setTemplatePath("template.pdf");
        attachment.setGenerated(true);

        Product product = createProductWithAttachment("PA", attachment);
        asserter.execute(() -> when(productService.getProductIsValid(anyString())).thenReturn(product));

        File pdf = new File("src/test/resources/test.pdf");

        DSSDocument document = new FileDocument(pdf);
        when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn(document);

        when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("fake-digest");


        asserter.execute(() -> when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(pdf));

        asserter.execute(
                () ->
                        when(azureBlobClient.uploadFile(anyString(), anyString(), any(byte[].class)))
                                .thenReturn("mocked-filepath"));

        FormItem formItem = FormItem.builder().file(pdf).fileName(filename).build();

        mockPersistToken(asserter);

        asserter.assertThat(
                () -> tokenService.uploadAttachment(onboardingId, formItem, filename),
                result -> {
                    verify(productService).getProductIsValid(anyString());
                    verify(azureBlobClient).getFileAsPdf(anyString());
                    verify(azureBlobClient).uploadFile(anyString(), anyString(), any(byte[].class));
                });
    }

    @Test
    @RunOnVertxContext
    void uploadAttachmentFailed(UniAsserter asserter) {

        final String onboardingId = "onboardingId";
        final String filename = "filename.pdf";
        final String productId = "productId";

        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);
        onboarding.setProductId(productId);

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);

        PanacheMock.mock(Onboarding.class);
        when(Onboarding.findById(onboardingId))
                .thenReturn(Uni.createFrom().item(onboarding));

        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().item(this::createDummyToken));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3",
                onboardingId,
                ATTACHMENT.name(),
                filename
        )).thenReturn(queryPage);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(
                () -> when(Onboarding.findById(anyString())).thenReturn(Uni.createFrom().item(onboarding)));

        AttachmentTemplate attachment = new AttachmentTemplate();
        attachment.setName(filename);
        attachment.setTemplatePath("template.pdf");
        attachment.setGenerated(true);

        Product product = createProductWithAttachment("PA", attachment);
        asserter.execute(() -> when(productService.getProductIsValid(anyString())).thenReturn(product));

        File pdf = new File("src/test/resources/test.pdf");

        asserter.execute(() -> when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(pdf));

        asserter.execute(
                () ->
                        when(azureBlobClient.uploadFile(anyString(), anyString(), any(byte[].class)))
                                .thenReturn("mocked-filepath"));

        FormItem formItem = FormItem.builder().file(pdf).fileName(filename).build();

        mockPersistToken(asserter);

        asserter.assertFailedWith(
                () -> tokenService.uploadAttachment(onboardingId, formItem, filename),
                it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException.class
        );

    }

    void mockPersistToken(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(
            () ->
                when(Token.persist(any(Token.class), any()))
                    .thenAnswer(
                        arg -> {
                          Token token = (Token) arg.getArguments()[0];
                          token.setId(UUID.randomUUID().toString());
                          return Uni.createFrom().nullItem();
                        }));
    }

    private Product createProductWithAttachment(
            String institutionType, AttachmentTemplate attachment) {
        Product product = new Product();
        product.setId("prod");

        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setAttachments(List.of(attachment));

        Map<String, ContractTemplate> institutionContractMappings = new HashMap<>();
        institutionContractMappings.put(institutionType, contractTemplate);

        product.setInstitutionContractMappings(institutionContractMappings);
        return product;
    }

    @Test
    void getTokenAttachments() {
        final String filename = "filename";
        Token token = new Token();
        token.setContractFilename(filename);
        token.setName(filename);
        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.stream()).thenReturn(Multi.createFrom().item(token));

        PanacheMock.mock(Token.class);
        when(Token.find("onboardingId = ?1 and type = ?2", onboardingId, ATTACHMENT.name()))
                .thenReturn(queryPage);

        UniAssertSubscriber<List<String>> subscriber =
                tokenService
                        .getAttachments(onboardingId)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        List<String> actual = subscriber.awaitItem().getItem();
        assertNotNull(actual);
        Assertions.assertFalse(actual.isEmpty());
        assertEquals(filename, actual.get(0));
    }


    @Test
    void updateContractSignedTest_OK() {
        // given
        String onboardingId = "testContractSigned";
        String documentSignedPath = "/test/";
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("contractSigned", documentSignedPath);

        ReactivePanacheUpdate queryPage = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Token.class);
        when(queryPage.where(anyString(), anyString())).thenReturn(Uni.createFrom().item(1L));
        when(Token.update(QueryUtils.buildUpdateDocument(testMap))).thenReturn(queryPage);

        // when
        UniAssertSubscriber<Long> result =
                tokenService
                        .updateContractSigned(onboardingId, documentSignedPath)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        // then
        assertNotNull(result);
        assertEquals(1L, result.assertCompleted().awaitItem().getItem());
    }

    @Test
    void updateContractSignedTest_KO() {
        // given
        String onboardingId = "testContractSigned";
        String documentSignedPath = "/test/";

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("contractSigned", documentSignedPath);

        ReactivePanacheUpdate queryPage = mock(ReactivePanacheUpdate.class);
        PanacheMock.mock(Token.class);
        when(queryPage.where(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(Long.valueOf("0")));
        when(Token.update(QueryUtils.buildUpdateDocument(testMap))).thenReturn(queryPage);

        // when
        UniAssertSubscriber<Long> result =
                tokenService
                        .updateContractSigned(onboardingId, documentSignedPath)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        // then
        assertNotNull(result);
        assertEquals(
                "Token with id testContractSigned not found or already deleted",
                result.getFailure().getMessage());
    }

    @Test
    void reportContractSignedTest_OK() {
        Token token = new Token();
        token.setContractFilename("fileName");
        token.setType(TokenType.INSTITUTION);

        PanacheMock.mock(Token.class);
        when(Token.findById(onboardingId)).thenReturn(Uni.createFrom().item(token));

        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(new File("fileName"));

        UniAssertSubscriber<ContractSignedReport> subscriber =
                tokenService
                        .reportContractSigned(onboardingId)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        ContractSignedReport actual = subscriber.awaitItem().getItem();
        assertNotNull(actual);
        // assertEquals(RestResponse.Status.OK.getStatusCode(), actual.getStatus());
    }

    private Token createDummyToken() {
        Token token = new Token();
        token.setId(UUID.randomUUID().toString());
        token.setProductId("prod-id");
        token.setContractSigned("file");
        return token;
    }

    private void mockFindToken(UniAsserter asserter, String tokenId) {
        Token token = new Token();
        token.setChecksum("actual-checksum");
        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(
                () -> when(Token.list("_id", tokenId)).thenReturn(Uni.createFrom().item(List.of(token))));
    }

    @Test
    @RunOnVertxContext
    void reportContractSignedTest(UniAsserter asserter) {
        Token token = createDummyToken();
        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(
                () ->
                        when(Token.findByIdOptional(any()))
                                .thenReturn(Uni.createFrom().item(Optional.of(token))));

        mockFindToken(asserter, token.getId());

        when(signatureService.verifySignature(any())).thenReturn(true);

        asserter.assertThat(
                () -> tokenService.reportContractSigned(token.getId()), Assertions::assertNotNull);
    }

    @Test
    void getTemplateAndVerifyDigestWithContractTemplateSuccess() {
        File uploadedFile = new File("src/test/resources/test.pdf");
        FormItem formItem = FormItem.builder().file(uploadedFile).fileName("contract.pdf").build();

        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("contracts/template.pdf");

        when(azureBlobClient.getFileAsPdf("contracts/template.pdf")).thenReturn(uploadedFile);

        DSSDocument document = new FileDocument(uploadedFile);
        when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn(document);

        when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("fake-digest");


        String result = tokenService.getTemplateAndVerifyDigest(formItem, contractTemplate.getContractTemplatePath(), false);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(azureBlobClient).getFileAsPdf("contracts/template.pdf");
    }

    @Test
    void getTemplateAndVerifyDigestWithContractTemplateDigestMismatch() {
        File uploadedFile = new File("src/test/resources/test.pdf");
        FormItem formItem = FormItem.builder().file(uploadedFile).fileName("contract.pdf").build();

        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("contracts/original.pdf");

        DSSDocument document = new FileDocument(uploadedFile);
        when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn(document);

        when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("fake-digest").thenReturn("fake-digest2");


        File differentFile = new File("src/test/resources/original.pdf");
        when(azureBlobClient.getFileAsPdf("contracts/original.pdf")).thenReturn(differentFile);

        try {
            tokenService.getTemplateAndVerifyDigest(formItem, contractTemplate.getContractTemplatePath(), false);
            Assertions.fail("Expected InvalidRequestException to be thrown");
        } catch (InvalidRequestException e) {
            assertTrue(e.getMessage().contains("File has been changed"));
        }
        verify(azureBlobClient).getFileAsPdf("contracts/original.pdf");
    }

    @Test
    void getTemplateAndVerifyDigestReturnsBase64EncodedDigest() {
        File uploadedFile = new File("src/test/resources/test.pdf");
        FormItem formItem = FormItem.builder().file(uploadedFile).fileName("test.pdf").build();

        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("contracts/test.pdf");

        when(azureBlobClient.getFileAsPdf("contracts/test.pdf")).thenReturn(uploadedFile);

        DSSDocument document = new FileDocument(uploadedFile);
        when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn(document);

        when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("rhfouednionew3");

        String result = tokenService.getTemplateAndVerifyDigest(formItem, contractTemplate.getContractTemplatePath(), false);

        assertNotNull(result);
        assertTrue(result.matches("^[A-Za-z0-9+/=]+$"));
    }

    @Test
    void getTemplateAndVerifyDigestThrowsInvalidRequestExceptionWithCorrectMessage() {
        File uploadedFile = new File("src/test/resources/test.pdf");
        FormItem formItem = FormItem.builder().file(uploadedFile).fileName("test.pdf").build();

        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("contracts/original.pdf");

        File differentFile = new File("src/test/resources/original.pdf");

        DSSDocument document = new FileDocument(uploadedFile);
        DSSDocument document2 = new FileDocument(differentFile);
        when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class)))
                .thenReturn(document)
                .thenReturn(document2);

        when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("fake-digest")
                .thenReturn("fake-digestt");

        when(azureBlobClient.getFileAsPdf("contracts/original.pdf")).thenReturn(differentFile);

        try {
            tokenService.getTemplateAndVerifyDigest(formItem, contractTemplate.getContractTemplatePath(), false);
            Assertions.fail("Expected InvalidRequestException to be thrown");
        } catch (InvalidRequestException e) {
            assertEquals("File has been changed. It's not possible to complete upload", e.getMessage());
        }
    }

    @Test
    void getTemplateAndVerifyDigestVerifiesAzureBlobClientIsCalled() {
        File uploadedFile = new File("src/test/resources/test.pdf");
        FormItem formItem = FormItem.builder().file(uploadedFile).fileName("test.pdf").build();

        ContractTemplate contractTemplate = new ContractTemplate();
        String templatePath = "contracts/template/path.pdf";
        contractTemplate.setContractTemplatePath(templatePath);

        when(azureBlobClient.getFileAsPdf(templatePath)).thenReturn(uploadedFile);

        DSSDocument document = new FileDocument(uploadedFile);
        when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn(document);
        when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("fake-digest");

        tokenService.getTemplateAndVerifyDigest(formItem, contractTemplate.getContractTemplatePath(), false);

        verify(azureBlobClient).getFileAsPdf(templatePath);
    }

    @Test
    void getTemplateAndVerifyDigestWithSameFileReturnsDigest() {
        File file = new File("src/test/resources/test.pdf");
        FormItem formItem = FormItem.builder().file(file).fileName("test.pdf").build();

        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("contracts/test.pdf");

        DSSDocument document = new FileDocument(file);
        when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn(document);

        when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("fake-digest");


        when(azureBlobClient.getFileAsPdf("contracts/test.pdf")).thenReturn(file);

        String result = tokenService.getTemplateAndVerifyDigest(formItem, contractTemplate.getContractTemplatePath(), false);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getTemplateAndVerifyDigestThrowsExceptionWhenFilesAreDifferent() {
        File uploadedFile = new File("src/test/resources/test.pdf");
        FormItem formItem = FormItem.builder().file(uploadedFile).fileName("test.pdf").build();

        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("contracts/original.pdf");

        File originalFile = new File("src/test/resources/original.pdf");
        when(azureBlobClient.getFileAsPdf("contracts/original.pdf")).thenReturn(originalFile);

        DSSDocument document = new FileDocument(uploadedFile);
        DSSDocument document2 = new FileDocument(originalFile);
        when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn(document).thenReturn(document2);

        when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("fake-digest").thenReturn("fake-digest2");

        Assertions.assertThrows(InvalidRequestException.class, () ->
                tokenService.getTemplateAndVerifyDigest(formItem, contractTemplate.getContractTemplatePath(), false)
        );
    }

    @Test
    @RunOnVertxContext
    void existsAttachmentTest_shouldReturnTrue(UniAsserter asserter) {
        // given
        String onboardingId = UUID.randomUUID().toString();
        String filename = "Addendum.pdf";

        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() ->
                when(Onboarding.findById(onboardingId))
                        .thenReturn(Uni.createFrom().item(onboarding))
        );

        Token token = createDummyToken();
        token.setContractFilename(filename);
        token.setName(filename);

        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().item(token));

        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() ->
                when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3",
                        onboardingId,
                        ATTACHMENT.name(),
                        filename
                )).thenReturn(queryPage)
        );

        when(azureBlobClient.getProperties(anyString())).thenReturn(null);

        // when / then
        asserter.assertThat(
                () -> tokenService.existsAttachment(onboardingId, filename),
                result -> {
                    assertNotNull(result);
                    assertTrue(result);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void existsAttachmentTest_shouldReturnFalse_whenNotFoundToken(UniAsserter asserter) {
        // given
        String onboardingId = UUID.randomUUID().toString();
        String filename = "filename";

        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() ->
                when(Onboarding.findById(onboardingId))
                        .thenReturn(Uni.createFrom().item(onboarding))
        );

        Token token = createDummyToken();
        token.setContractFilename(filename);
        token.setName(filename);

        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().nullItem());

        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() ->
                when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3",
                        onboardingId,
                        ATTACHMENT.name(),
                        filename
                )).thenReturn(queryPage)
        );

        // when / then
        asserter.assertThat(
                () -> tokenService.existsAttachment(onboardingId, filename),
                result -> {
                    assertNotNull(result);
                    assertFalse(result);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void existsAttachmentTest_shouldReturnFalse_whenNotOnBlobStorage(UniAsserter asserter) {
        // given
        String onboardingId = UUID.randomUUID().toString();
        String filename = "filename";

        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);

        asserter.execute(() -> PanacheMock.mock(Onboarding.class));
        asserter.execute(() ->
                when(Onboarding.findById(onboardingId))
                        .thenReturn(Uni.createFrom().item(onboarding))
        );

        Token token = createDummyToken();
        token.setContractFilename(filename);
        token.setName(filename);

        ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
        when(queryPage.firstResult()).thenReturn(Uni.createFrom().item(token));

        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() ->
                when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3",
                        onboardingId,
                        ATTACHMENT.name(),
                        filename
                )).thenReturn(queryPage)
        );

        when(azureBlobClient.getProperties(anyString()))
                .thenThrow(new SelfcareAzureStorageException("0000", "Error"));

        // when / then
        asserter.assertThat(
                () -> tokenService.existsAttachment(onboardingId, filename),
                result -> {
                    assertNotNull(result);
                    assertFalse(result);
                }
        );
    }

    @Test
    void createSafeTempFile() throws Exception {
        Path path = tokenService.createSafeTempFile();

        assertNotNull(path);
        File file = path.toFile();
        assertTrue(file.exists());
        assertTrue(file.getName().startsWith("signed"));
        assertTrue(file.getName().endsWith(".pdf"));

        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            java.util.Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
            assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
            assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
            assertEquals(2, permissions.size(), "Permissions should only be OWNER_READ and OWNER_WRITE");
        } else {
            assertTrue(file.canRead());
            assertTrue(file.canWrite());
        }

        Files.deleteIfExists(path);
    }

    @Test
    void createSafeTempFileUnsupportedOperationException() throws Exception {
        TokenServiceDefault serviceSpy = spy(tokenService);
        doThrow(new UnsupportedOperationException("forced"))
                .when(serviceSpy).createTempFileWithPosix();

        Path path = serviceSpy.createSafeTempFile();

        assertNotNull(path);
        File file = path.toFile();
        assertTrue(file.exists());
        assertTrue(file.canRead());
        assertTrue(file.canWrite());

        Files.deleteIfExists(path);
    }

    public static class SignPdfProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "onboarding-ms.pagopa-signature.source", "true");
        }
    }

    @Nested
    @TestProfile(SignPdfProfile.class)
    class SignAttachmentTest {

        @Test
        @RunOnVertxContext
        void uploadAttachmentSuccessWithSignature(UniAsserter asserter) {
            final String onboardingId = "onboardingId";
            final String filename = "filename.pdf";
            final String productId = "productId";

            Onboarding onboarding = new Onboarding();
            onboarding.setId(onboardingId);
            onboarding.setProductId(productId);

            Institution institution = new Institution();
            institution.setInstitutionType(InstitutionType.PA);
            institution.setDescription("description");
            onboarding.setInstitution(institution);

            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(() ->
                    when(Onboarding.findById(onboardingId))
                            .thenReturn(Uni.createFrom().item(onboarding))
            );

            ReactivePanacheQuery queryPage = mock(ReactivePanacheQuery.class);
            when(queryPage.firstResult()).thenReturn(Uni.createFrom().nullItem());

            asserter.execute(() -> PanacheMock.mock(Token.class));
            asserter.execute(() ->
                    when(Token.find("onboardingId = ?1 and type = ?2 and name = ?3",
                            onboardingId,
                            ATTACHMENT.name(),
                            filename
                    )).thenReturn(queryPage)
            );

            asserter.execute(() -> PanacheMock.mock(Onboarding.class));
            asserter.execute(
                    () ->
                            when(Onboarding.findById(anyString())).thenReturn(Uni.createFrom().item(onboarding)));

            AttachmentTemplate attachment = new AttachmentTemplate();
            attachment.setName(filename);
            attachment.setTemplatePath("template.pdf");
            attachment.setGenerated(true);

            Product product = createProductWithAttachment("PA", attachment);
            asserter.execute(
                    () -> when(productService.getProductIsValid(anyString())).thenReturn(product));

            File pdf = new File("src/test/resources/test.pdf");

            asserter.execute(() -> when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(pdf));

            asserter.execute(
                    () ->
                            when(azureBlobClient.uploadFile(anyString(), anyString(), any(byte[].class)))
                                    .thenReturn("mocked-filepath"));

            asserter.execute(() ->
                    doNothing().when(padesSignService)
                            .padesSign(any(), any(), any())
            );

            FormItem formItem = FormItem.builder().file(pdf).fileName(filename).build();


            DSSDocument document = new FileDocument(pdf);
            when(signatureService.extractPdfFromSignedContainer(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn(document);

            when(signatureService.computeDigestOfSignedRevision(any(SignedDocumentValidator.class), any(DSSDocument.class))).thenReturn("fake-digest");

            mockPersistToken(asserter);

            asserter.assertThat(
                    () -> tokenService.uploadAttachment(onboardingId, formItem, filename),
                    result -> {
                        verify(productService).getProductIsValid(anyString());
                        verify(azureBlobClient).getFileAsPdf(anyString());
                        verify(azureBlobClient).uploadFile(anyString(), anyString(), any(byte[].class));
                    });
        }

        @Test
        void retrieveAttachmentSuccessAndSignTest() throws Exception {
            // given
            final String onboardingId = "onboardingId";
            final String filename = "filename.pdf";
            final String productId = "productId";
            final String institutionType = "PA";

            Onboarding onboarding = new Onboarding();
            onboarding.setId(onboardingId);
            onboarding.setProductId(productId);

            Institution institution = new Institution();
            institution.setInstitutionType(InstitutionType.PA);
            institution.setDescription("description");
            onboarding.setInstitution(institution);

            PanacheMock.mock(Onboarding.class);
            when(Onboarding.findById(onboardingId)).thenReturn(Uni.createFrom().item(onboarding));

            AttachmentTemplate attachment = new AttachmentTemplate();
            attachment.setName(filename);
            attachment.setGenerated(false);
            attachment.setTemplatePath("templatePath");

            Product product = createProductWithAttachment(institutionType, attachment);
            product.setId(productId);

            when(productService.getProductIsValid(productId)).thenReturn(product);

            File inputFile = Files.createTempFile("input", ".pdf").toFile();
            inputFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(inputFile)) {
                fos.write("dummy-content".getBytes(StandardCharsets.UTF_8));
            }

            when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(inputFile);

            doAnswer(invocation -> {
                File in = invocation.getArgument(0);
                File out = invocation.getArgument(1);
                try (InputStream is = new FileInputStream(in);
                     OutputStream os = new FileOutputStream(out)) {
                    is.transferTo(os);
                }
                return null;
            }).when(padesSignService).padesSign(any(File.class), any(File.class), any());

            // when
            UniAssertSubscriber<RestResponse<File>> subscriber =
                    tokenService.retrieveTemplateAttachment(onboardingId, filename)
                            .subscribe().withSubscriber(UniAssertSubscriber.create());

            // then
            RestResponse<File> response = subscriber.awaitItem().getItem();

            assertNotNull(response);
            assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());

            File signedFile = response.getEntity();
            assertNotNull(signedFile);
            assertTrue(signedFile.exists());

            assertNotEquals(inputFile.getAbsolutePath(), signedFile.getAbsolutePath());

            byte[] origBytes = Files.readAllBytes(inputFile.toPath());
            byte[] signedBytes = Files.readAllBytes(signedFile.toPath());
            assertArrayEquals(origBytes, signedBytes);

            verify(productService).getProductIsValid(productId);
            verify(azureBlobClient).getFileAsPdf(anyString());
            verify(padesSignService).padesSign(any(File.class), any(File.class), any());
        }
    }

    @Test
    @RunOnVertxContext
    void retrieveToken_whenTokenListIsEmpty_shouldCreateNewToken(UniAsserter asserter) {
        String productId = "prod-id";
        String institutionType = "PA";

        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);
        onboarding.setProductId(productId);

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);

        File pdf = new File("src/test/resources/test.pdf");
        FormItem formItem = FormItem.builder()
                .file(pdf)
                .fileName("contract.pdf")
                .build();

        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("contracts/template.pdf");

        Product product = new Product();
        product.setId(productId);
        Map<String, ContractTemplate> mappings = new HashMap<>();
        mappings.put(institutionType, contractTemplate);
        product.setInstitutionContractMappings(mappings);

        Token mappedToken = new Token();
        mappedToken.setId(UUID.randomUUID().toString());
        mappedToken.setOnboardingId(onboardingId);

        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() ->
                when(Token.list("onboardingId", onboardingId))
                        .thenReturn(Uni.createFrom().item(Collections.emptyList()))
        );
        asserter.execute(() ->
                when(tokenMapper.toModel(onboarding, product, contractTemplate))
                    .thenReturn(mappedToken));

        asserter.execute(() -> when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(pdf));

        mockPersistToken(asserter);

        asserter.assertThat(
                () -> tokenService.retrieveToken(onboarding, formItem, product),
                result -> {
                    assertNotNull(result);
                    assertEquals(onboardingId, result.getOnboardingId());
                    assertNotNull(result.getChecksum());
                }
        );
    }

    @Test
    @RunOnVertxContext
    void retrieveToken_whenTokenListIsNotEmpty_shouldReturnFirstToken(UniAsserter asserter) {
        String productId = "prod-id";

        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);
        onboarding.setProductId(productId);

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);

        File pdf = new File("src/test/resources/test.pdf");
        FormItem formItem = FormItem.builder()
                .file(pdf)
                .fileName("contract.pdf")
                .build();

        Product product = new Product();
        product.setId(productId);

        Token existingToken = new Token();
        existingToken.setId(UUID.randomUUID().toString());
        existingToken.setOnboardingId(onboardingId);
        existingToken.setProductId(productId);
        existingToken.setChecksum("existing-checksum");

        asserter.execute(() -> PanacheMock.mock(Token.class));
        asserter.execute(() ->
                when(Token.list("onboardingId", onboardingId))
                        .thenReturn(Uni.createFrom().item(List.of(existingToken)))
        );

        asserter.assertThat(
                () -> tokenService.retrieveToken(onboarding, formItem, product),
                result -> {
                    assertNotNull(result);
                    assertEquals(existingToken.getId(), result.getId());
                    assertEquals(existingToken.getChecksum(), result.getChecksum());
                    assertEquals(onboardingId, result.getOnboardingId());
                }
        );
    }

    @Test
    void retrieveToken_shouldReturnFirstToken() {
        Token expectedToken = new Token();
        expectedToken.setId(UUID.randomUUID().toString());
        expectedToken.setOnboardingId(onboardingId);
        expectedToken.setProductId("prod-id");

        PanacheMock.mock(Token.class);
        when(Token.list("onboardingId", onboardingId))
                .thenReturn(Uni.createFrom().item(List.of(expectedToken)));

        UniAssertSubscriber<Token> subscriber = tokenService
                .retrieveToken(onboardingId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        Token actualToken = subscriber.awaitItem().getItem();
        assertNotNull(actualToken);
        assertEquals(expectedToken.getId(), actualToken.getId());
        assertEquals(expectedToken.getOnboardingId(), actualToken.getOnboardingId());
        assertEquals(expectedToken.getProductId(), actualToken.getProductId());
    }

    @Test
    void updateTokenWithFilePath() {
        String filepath = "/path/to/file.pdf";
        Token token = new Token();
        token.setId(UUID.randomUUID().toString());
        token.setOnboardingId(onboardingId);

        ReactivePanacheUpdate updateQuery = mock(ReactivePanacheUpdate.class);
        when(updateQuery.where("_id", token.getId()))
                .thenReturn(Uni.createFrom().item(1L));

        PanacheMock.mock(Token.class);
        when(Token.update("contractSigned", filepath))
                .thenReturn(updateQuery);

        UniAssertSubscriber<String> subscriber = tokenService
                .updateTokenWithFilePath(filepath, token)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        String result = subscriber.awaitItem().getItem();
        assertNotNull(result);
        assertEquals(filepath, result);
    }

    @Test
    void updateTokenUpdatedAt() {
        ReactivePanacheUpdate updateQuery = mock(ReactivePanacheUpdate.class);
        when(updateQuery.where("onboardingId", onboardingId))
                .thenReturn(Uni.createFrom().item(1L));

        PanacheMock.mock(Token.class);
        when(Token.update(eq("updatedAt"), any(Object.class)))
                .thenReturn(updateQuery);

        UniAssertSubscriber<Void> subscriber = tokenService
                .updateTokenUpdatedAt(onboardingId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitItem().assertCompleted();
        verify(updateQuery).where("onboardingId", onboardingId);
    }

    @Test
    @RunOnVertxContext
    void persistTokenForImport(UniAsserter asserter) {
        // given
        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboardingId);

        Product product = new Product();
        product.setId("prod-id");

        OnboardingImportContract contractImported = new OnboardingImportContract();

        Token mappedToken = new Token();
        mappedToken.setId(UUID.randomUUID().toString());
        mappedToken.setOnboardingId(onboardingId);

        when(tokenMapper.toModel(onboarding, product, contractImported)).thenReturn(mappedToken);

        asserter.execute(() -> PanacheMock.mock(Token.class));
        mockPersistToken(asserter);

        asserter.assertThat(
            () -> tokenService.persistTokenForImport(onboarding, product, contractImported),
            result -> {
              verify(tokenMapper).toModel(onboarding, product, contractImported);
            });
    }
}
