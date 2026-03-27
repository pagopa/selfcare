package it.pagopa.selfcare.onboarding.service.impl;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.onboarding.conf.OnboardingMsConfig;
import it.pagopa.selfcare.onboarding.conf.PagoPaSignatureConfig;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.onboarding.mapper.TokenMapper;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.service.SignatureService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.onboarding.util.Utils;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.util.*;

import static it.pagopa.selfcare.onboarding.common.DocumentType.ATTACHMENT;
import static it.pagopa.selfcare.onboarding.constants.CustomError.TOKEN_NOT_FOUND_OR_ALREADY_DELETED;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.*;

@Slf4j
@ApplicationScoped
public class TokenServiceDefault implements TokenService {

    public static final String HTTP_HEADER_VALUE_ATTACHMENT_FILENAME = "attachment;filename=";
    public static final String PAGOPA_SIGNATURE_DISABLED = "disabled";
    private static final String ONBOARDING_ID = "onboardingId";

    private final AzureBlobClient azureBlobClient;
    private final OnboardingMsConfig onboardingMsConfig;
    private final ProductService productService;
    private final PagoPaSignatureConfig pagoPaSignatureConfig;
    private final PadesSignService padesSignService;

    @Inject
    SignatureService signatureService;
    @Inject
    TokenMapper tokenMapper;
    @ConfigProperty(name = "onboarding-ms.signature.verify-enabled")
    Boolean isVerifyEnabled;
    @ConfigProperty(name = "onboarding-ms.blob-storage.path-contracts")
    String pathContracts;

    public TokenServiceDefault(AzureBlobClient azureBlobClient,
                               OnboardingMsConfig onboardingMsConfig,
                               ProductService productService,
                               PagoPaSignatureConfig pagoPaSignatureConfig,
                               PadesSignService padesSignService) {
        this.azureBlobClient = azureBlobClient;
        this.onboardingMsConfig = onboardingMsConfig;
        this.productService = productService;
        this.pagoPaSignatureConfig = pagoPaSignatureConfig;
        this.padesSignService = padesSignService;
    }

    public static void isP7mValid(File contract, SignatureService signatureService) {
        signatureService.verifySignature(contract);
    }

    public static void isPdfValid(File contract) {
        try (PDDocument document = Loader.loadPDF(contract)) {
            if (document.getNumberOfPages() == 0) {
                throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
            }
        } catch (IOException e) {
            throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
        }
    }

    private static String getCurrentContractName(Token token, boolean isSigned) {
        return isSigned ? getContractSignedName(token) : token.getContractFilename();
    }

    private static String getContractSignedName(Token token) {
        File file = new File(token.getContractSigned());
        return file.getName();
    }

    @Override
    public Uni<List<Token>> getToken(String onboardingId) {
        return Token.find(ONBOARDING_ID, onboardingId)
                .list();
    }

    @Override
    public Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned) {
        return Token.findById(onboardingId)
                .map(Token.class::cast)
                .onItem().transformToUni(token ->
                        Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(isSigned ? token.getContractSigned() : getContractNotSigned(onboardingId, token)))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                                .onItem().transform(contract -> {
                                    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(contract, MediaType.APPLICATION_OCTET_STREAM);
                                    response.header(HttpHeaders.CONTENT_DISPOSITION, HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + getCurrentContractName(token, isSigned));
                                    return response.build();
                                }));
    }

    @Override
    public Uni<RestResponse<File>> retrieveSignedFile(String onboardingId) {
        return Token.findById(onboardingId)
                .map(Token.class::cast)
                .onItem().transformToUni(token -> Uni.createFrom().item(() -> azureBlobClient.retrieveFile(token.getContractSigned()))
                        .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                        .onItem().transform(contract -> {
                            File fileToSend = contract;
                            if (token.getContractSigned().endsWith(".pdf")) {
                                isPdfValid(contract);
                            } else {
                                isP7mValid(contract, signatureService);
                                fileToSend = signatureService.extractFile(contract);
                                isPdfValid(fileToSend);
                            }
                            RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(fileToSend, MediaType.APPLICATION_OCTET_STREAM);
                            String filename = getCurrentContractName(token, true);
                            response.header(HttpHeaders.CONTENT_DISPOSITION, HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename);
                            return response.build();
                        }).onFailure().recoverWithUni(() -> Uni.createFrom().item(RestResponse.ResponseBuilder.<File>notFound().build())));
    }

    private String getContractNotSigned(String onboardingId, Token token) {
        return String.format("%s%s/%s", onboardingMsConfig.getContractPath(), onboardingId,
                token.getContractFilename());
    }

    @Override
    public Uni<RestResponse<File>> retrieveTemplateAttachment(String onboardingId, String attachmentName) {
        return findOnboardingById(onboardingId)
                .onItem().transformToUni(onboarding -> {

                    Product product = productService.getProductIsValid(onboarding.getProductId());
                    AttachmentTemplate attachment = getAttachmentTemplate(attachmentName, onboarding, product);

                    return Uni.createFrom()
                            .item(() -> azureBlobClient.getFileAsPdf(attachment.getTemplatePath()))
                            .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Template Attachment not found on storage for onboarding: %s", onboardingId)))
                            .chain(file -> signDocument(file, onboarding.getInstitution().getDescription(),
                                    product.getId()))
                            .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                            .onItem().transform(file -> RestResponse.ResponseBuilder
                                    .ok(file, MediaType.APPLICATION_OCTET_STREAM)
                                    .header(HttpHeaders.CONTENT_DISPOSITION,
                                            HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + attachmentName)
                                    .build());
                });
    }

    @Override
    public Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName) {
        return Token
                .find(
                        "onboardingId = ?1 and type = ?2 and name = ?3",
                        onboardingId,
                        ATTACHMENT.name(),
                        attachmentName
                )
                .firstResult()
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Token with id %s not found", onboardingId)))
                .map(Token.class::cast)
                .onItem().transformToUni(token ->
                        Uni.createFrom()
                                .item(() -> azureBlobClient.getFileAsPdf(buildAttachmentPath(token)
                                ))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                                .onItem().transform(contract -> RestResponse.ResponseBuilder
                                        .ok(contract, MediaType.APPLICATION_OCTET_STREAM)
                                        .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + token.getContractFilename()
                                        )
                                        .build())
                );
    }

    @Override
    public Uni<Void> uploadAttachment(String onboardingId, FormItem formItem, String attachmentName) {
        return existsAttachment(onboardingId, attachmentName)
                .onItem().transformToUni(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Uni.createFrom().failure(new UpdateNotAllowedException(ATTACHMENT_UPLOAD_ERROR.getCode(), ATTACHMENT_UPLOAD_ERROR.getMessage()));
                    }
                    return Uni.createFrom().voidItem();
                })
                .chain(() -> findOnboardingById(onboardingId))
                .chain(onboarding -> {
                    Product product = productService.getProductIsValid(onboarding.getProductId());
                    AttachmentTemplate attachment = this.getAttachmentTemplate(attachmentName, onboarding, product);

                    if (Boolean.TRUE.equals(isVerifyEnabled)) {
                        signatureService.verifySignature(formItem.getFile());
                    }

                    String digest = getTemplateAndVerifyDigest(formItem, attachment.getTemplatePath(), false);

                    File fileToUpload = formItem.getFile();

                    boolean isP7M = Optional.of(formItem.getFileName())
                            .map(name -> name.toLowerCase(Locale.ROOT).endsWith(".p7m"))
                            .orElse(false);

                    return persistTokenAttachment(onboardingId, attachment, digest, isP7M)
                            .onItem().invoke(token ->
                                    uploadFileToAzure(
                                            token.getContractFilename(),
                                            onboardingId,
                                            fileToUpload
                                    )
                            );
                })
                .replaceWithVoid();
    }

    public String getAndVerifyDigest(FormItem file, ContractTemplate contract, boolean skipDigestCheck) {
        return getAndVerifyDigestByTemplate(file, contract.getContractTemplatePath(), skipDigestCheck);
    }

    private String getAndVerifyDigestByTemplate(FormItem file, String templatePath, boolean skipDigestCheck) {
        DSSDocument document = new FileDocument(file.getFile());
        String digest = document.getDigest(DigestAlgorithm.SHA256).getBase64Value();
        File originalFile = azureBlobClient.getFileAsPdf(templatePath);
        DSSDocument originalDocument = new FileDocument(originalFile);
        String originalDigest = originalDocument.getDigest(DigestAlgorithm.SHA256).getBase64Value();
        if (!digest.equals(originalDigest) && !skipDigestCheck) {
            throw new InvalidRequestException(
                    "File has been changed. It's not possible to complete upload");
        }
        return digest;
    }

    public String getTemplateAndVerifyDigest(FormItem file, String documentTemplatePath, boolean skipDigestCheck) {
        log.info("Start verifying uploaded content against template (templatePath={})", documentTemplatePath);
        Objects.requireNonNull(file, "Uploaded file must not be null");
        Objects.requireNonNull(documentTemplatePath, "Document template path must not be null");

        DSSDocument uploadedDocument = new FileDocument(file.getFile());

        File templateFile = azureBlobClient.getFileAsPdf(documentTemplatePath);
        DSSDocument templateDocument = new FileDocument(templateFile);

        DSSDocument uploadedPdf = signatureService.extractPdfFromSignedContainer(SignedDocumentValidator.fromDocument(uploadedDocument), uploadedDocument);
        DSSDocument templatePdf = signatureService.extractPdfFromSignedContainer(SignedDocumentValidator.fromDocument(templateDocument), templateDocument);

        SignedDocumentValidator uploadedPdfValidator = SignedDocumentValidator.fromDocument(uploadedPdf);
        SignedDocumentValidator templatePdfValidator = SignedDocumentValidator.fromDocument(templatePdf);

        String templateDigest = signatureService.computeDigestOfSignedRevision(templatePdfValidator, templatePdf);
        String uploadedDigest = signatureService.computeDigestOfSignedRevision(uploadedPdfValidator, uploadedPdf);

        log.debug("Template content digest (base64): {}", templateDigest);
        log.debug("Uploaded  content digest (base64): {}", uploadedDigest);

        if (!templateDigest.equals(uploadedDigest)) {
            log.warn("Content mismatch ignoring signatures. templateDigest={} uploadedDigest={}", templateDigest, uploadedDigest);
            if (!skipDigestCheck) {
                throw new InvalidRequestException("File has been changed. It's not possible to complete upload");
            }
        } else {
            log.info("Content check passed (ignoring signatures).");
        }

        return uploadedDigest;
    }


    private Uni<File> signDocument(File pdf, String institutionDescription, String productId) {
        return Uni.createFrom().item(() -> {
                    try {
                        if (PAGOPA_SIGNATURE_DISABLED.equals(pagoPaSignatureConfig.source())) {
                            log.info("Skipping PagoPA contract pdf sign due to global disabling");
                            return pdf;
                        }

                        String signReason =
                                pagoPaSignatureConfig
                                        .applyOnboardingTemplateReason()
                                        .replace("${institutionName}", institutionDescription)
                                        .replace("${productName}", productId);

                        log.info("Signing input file {} using reason {}", pdf.getName(), signReason);

                        Path signedPdf = createSafeTempFile();
                        padesSignService.padesSign(pdf, signedPdf.toFile(), buildSignatureInfo(signReason));
                        return signedPdf.toFile();

                    } catch (IOException e) {
                        throw new IllegalArgumentException("Impossible to sign pdf. Error: " + e.getMessage(), e);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private SignatureInformation buildSignatureInfo(String signReason) {
        return new SignatureInformation(
                pagoPaSignatureConfig.signer(), pagoPaSignatureConfig.location(), signReason);
    }

    @Override
    public Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath) {

        Map<String, Object> queryParameter = new HashMap<>();
        queryParameter.put("contractSigned", documentSignedPath);
        Document query = QueryUtils.buildUpdateDocument(queryParameter);

        return Token.update(query)
                .where("_id", onboardingId)
                .onItem()
                .transformToUni(
                        updateItemCount -> {
                            if (updateItemCount == 0) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format(TOKEN_NOT_FOUND_OR_ALREADY_DELETED.getMessage(), onboardingId)));
                            }
                            return Uni.createFrom().item(updateItemCount);
                        });
    }

    public Uni<List<String>> getAttachments(String onboardingId) {
        return Token.find("onboardingId = ?1 and type = ?2", onboardingId, ATTACHMENT.name())
                .stream().onItem().transform(Token.class::cast)
                .map(Token::getName)
                .collect().asList();
    }

    private String getAttachmentByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s%s", onboardingMsConfig.getContractPath(), onboardingId, "/attachments", "/" + filename);
    }

    public String getContractPathByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s", onboardingMsConfig.getContractPath(), onboardingId, "/" + filename);
    }

    private String buildAttachmentPath(Token token) {
        return Objects.nonNull(token.getContractSigned()) ? token.getContractSigned() : getAttachmentByOnboarding(token.getOnboardingId(), token.getContractFilename());
    }

    @Override
    public Uni<ContractSignedReport> reportContractSigned(String onboardingId) {
        return Token.findById(onboardingId)
                .map(Token.class::cast)
                .onItem().transformToUni(token ->
                        Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(token.getContractSigned()))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                                .onItem().transform(contract -> {
                                    signatureService.verifySignature(contract);
                                    return ContractSignedReport.cades(true);
                                })).onFailure().recoverWithUni(() -> Uni.createFrom().item(ContractSignedReport.cades(false)));
    }

    private AttachmentTemplate getAttachmentTemplate(String attachmentName, Onboarding onboarding, Product product) {
        return product
                .getInstitutionContractMappings()
                .get(onboarding.getInstitution().getInstitutionType().name())
                .getAttachments()
                .stream()
                .filter(a -> a.getName().equals(attachmentName))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                String.format("Attachment with name %s not found", attachmentName)
                        )
                );
    }

    private Uni<Token> persistTokenAttachment(String onboardingId, AttachmentTemplate attachment, String digest, boolean isP7M) {
        Token token = new Token();
        token.setId(UUID.randomUUID().toString());
        token.setCreatedAt(LocalDateTime.now());
        token.setActivatedAt(LocalDateTime.now());
        token.setType(ATTACHMENT);
        token.setOnboardingId(onboardingId);

        token.setContractVersion(attachment.getTemplateVersion());
        token.setContractTemplate(attachment.getTemplatePath());

        token.setName(attachment.getName());

        String signedContractFileName = Utils.extractFileName(token.getContractTemplate());
        String filename = String.format("signed_%s", signedContractFileName);

        if (isP7M) {
            filename = String.format("%s.p7m", filename);
        }

        token.setContractFilename(filename);

        token.setContractSigned(getAttachmentByOnboarding(
                onboardingId,
                token.getContractFilename()
        ));

        token.setChecksum(digest);
        return Token.persist(token).replaceWith(token);
    }

    private Uni<Onboarding> findOnboardingById(String onboardingId) {
        return Onboarding.findById(onboardingId)
                .onItem().ifNull().failWith(() ->
                        new ResourceNotFoundException(String.format("Onboarding with id %S not found", onboardingId))
                )
                .map(Onboarding.class::cast);
    }

    private void uploadFileToAzure(String filename, String onboardingId, File signedFile) throws OnboardingNotAllowedException {
        final String path = String.format("%s%s", pathContracts, onboardingId).concat("/attachments");

        try {
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(signedFile.toPath()));
        } catch (IOException e) {
            throw new OnboardingNotAllowedException(GENERIC_ERROR.getCode(),
                    "Error on upload contract for onboarding with id " + onboardingId);
        }
    }

    public Uni<Boolean> existsAttachment(String onboardingId, String attachmentName) {
        return findOnboardingById(onboardingId)
                .onItem().transformToUni(onboarding ->
                        {
                            String id = onboarding.getId();
                            return Token.find(
                                            "onboardingId = ?1 and type = ?2 and name = ?3",
                                            id,
                                            ATTACHMENT.name(),
                                            attachmentName
                                    )
                                    .firstResult()
                                    .map(Token.class::cast)
                                    .onItem().transformToUni(token -> {
                                        if (Objects.isNull(token)) {
                                            log.info("Token not found onboardingId={}, attachmentName={}", id, attachmentName);
                                            return Uni.createFrom().item(false);
                                        }

                                        return Uni.createFrom()
                                                .item(() -> {
                                                    try {
                                                        azureBlobClient.getProperties(token.getContractSigned());

                                                        log.info(
                                                                "Attachment found in storage onboardingId={}, attachmentName={}",
                                                                id,
                                                                attachmentName
                                                        );

                                                        return true;
                                                    } catch (SelfcareAzureStorageException e) {
                                                        log.info(
                                                                "Attachment not found in storage onboardingId={}, attachmentName={}",
                                                                id,
                                                                attachmentName
                                                        );
                                                        return false;
                                                    }
                                                })
                                                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
                                    });
                        }
                );
    }

    @Override
    public Uni<Token> retrieveToken(String onboardingId) {
        log.info("Retrieving token for onboardingId={}", onboardingId);
        return Token.list(ONBOARDING_ID, onboardingId)
                .map(tokens -> tokens.stream().findFirst().map(Token.class::cast).orElseThrow());
    }

    @Override
    public Uni<Token> retrieveToken(Onboarding onboarding, FormItem formItem, Product product) {
        String onboardingId = onboarding.getId();
        log.info("Retrieving token for onboardingId={}", onboardingId);
        return Token.list(ONBOARDING_ID, onboardingId)
                .flatMap(tokens -> {
                    if (tokens.isEmpty()) {
                        log.info("No token found for onboardingId={}, creating new token", onboardingId);
                        return createAndConfigureToken(onboarding, formItem, product);
                    }
                    return Uni.createFrom().item((Token) tokens.get(0));
                });
    }

    @Override
    public Uni<String> updateTokenWithFilePath(String filepath, Token token) {
        log.info("Updating token with id {} with file path {}", token.getId(), filepath);
        return Token.update("contractSigned", filepath)
                .where("_id", token.getId())
                .replaceWith(filepath);
    }

    @Override
    public Uni<Void> updateTokenUpdatedAt(String onboardingId) {
        log.info("Updating token 'updatedAt' for onboardingId={}", onboardingId);
        return Token.update("updatedAt", LocalDateTime.now())
            .where(ONBOARDING_ID, onboardingId)
            .onItem()
            .transform(ignore -> null);
    }

    @Override
    public Uni<Void> persistTokenForImport(Onboarding onboardingPersisted, Product product, OnboardingImportContract contractImported) {
        log.info("Persisting token for onboardingId={}", onboardingPersisted.getId());
        return Token.persist(tokenMapper.toModel(onboardingPersisted, product, contractImported));
    }

    private Uni<Token> createAndConfigureToken(Onboarding onboarding, FormItem formItem, Product product) {
        String onboardingId = onboarding.getId();
        String institutionType = onboarding.getInstitution().getInstitutionType().name();
        ContractTemplate contractTemplate = getContractTemplate(institutionType, product);
        String digest = getAndVerifyDigest(formItem, contractTemplate, true);
        Token token = tokenMapper.toModel(onboarding, product, contractTemplate);
        token.setContractSigned(getContractPathByOnboarding(onboardingId, formItem.getFileName()));
        token.setContractFilename(formItem.getFileName());
        token.setChecksum(digest);
        return Token.persist(token).replaceWith(token);
    }

    private ContractTemplate getContractTemplate(String institutionType, Product product) {
        return product.getInstitutionContractTemplate(institutionType);
    }

    public Path createSafeTempFile() throws IOException {
        try {
            return createTempFileWithPosix();
        } catch (UnsupportedOperationException e) {
            // Fallback per Windows/Non-POSIX
            File f = Files.createTempFile("signed", ".pdf").toFile();
            boolean readable = f.setReadable(true, true);
            boolean writable = f.setWritable(true, true);
            boolean executable = f.setExecutable(false); // Importante: NO esecuzione
            if (!readable || !writable || !executable) {
                log.warn("Could not set restricted permissions on temporary file: {}", f.getAbsolutePath());
            }
            return f.toPath();
        }
    }

    public Path createTempFileWithPosix() throws IOException {
        FileAttribute<Set<PosixFilePermission>> attr =
                PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rw-------")
                );
        return Files.createTempFile("signed", ".pdf", attr);
    }
}
