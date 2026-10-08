package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.*;

import it.pagopa.selfcare.onboarding.client.model.AttachmentTemplate;
import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.client.model.StorageOrigin;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final OnboardingService onboardingMsConnector;
    private final DocumentService documentMsClient;
    private final ProductService productService;
    private final OnboardingMapper onboardingMapper;

    private static final String ONBOARDING_ID_REQUIRED_MESSAGE = "OnboardingId is required";
    private static final String TOKEN_ID_IS_REQUIRED = "TokenId is required";

    @Override
    public OnboardingData verifyOnboarding(String onboardingId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding id = {}", onboardingId);
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        OnboardingData onboardingData = onboardingMapper.toOnboardingData(onboardingMsConnector.getOnboarding(onboardingId));
        log.debug("verifyOnboarding result = success");
        log.trace("verifyOnboarding end");
        return onboardingData;
    }

    @Override
    public void approveOnboarding(String onboardingId, String userUid) {
        log.trace("approveOnboarding start");
        log.debug("approveOnboarding id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        onboardingMsConnector.approveOnboarding(onboardingId, userUid);
        log.debug("approveOnboarding result = success");
        log.trace("approveOnboarding end");
    }

    @Override
    public void rejectOnboarding(String onboardingId, String reason, String userUid) {
        log.trace("rejectOnboarding start");
        log.debug("rejectOnboarding id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        onboardingMsConnector.rejectOnboarding(onboardingId, reason, userUid);
        log.debug("rejectOnboarding result = success");
        log.trace("rejectOnboarding end");
    }

    @Override
    public OnboardingData getOnboardingWithUserInfo(String onboardingId) {
        log.trace("getOnboardingWithUserInfo start");
        log.debug("getOnboardingWithUserInfo id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        OnboardingData onboardingData = onboardingMapper.toOnboardingData(onboardingMsConnector.getOnboardingWithUserInfo(onboardingId));
        log.debug("getOnboardingWithUserInfo result = success");
        log.trace("getOnboardingWithUserInfo end");
        return onboardingData;
    }

    @Override
    public void completeTokenV2(String onboardingId, UploadedFile contract) {
        log.trace("completeTokenAsync start");
        log.debug("completeTokenAsync id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        onboardingMsConnector.onboardingTokenComplete(onboardingId, contract);
        log.debug("completeTokenAsync result = success");
        log.trace("completeTokenAsync end");
    }

    @Override
    public void completeOnboardingUsers(String onboardingId, UploadedFile contract) {
        log.trace("completeOnboardingUsersAsync start");
        log.debug("completeOnboardingUsersAsync id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        onboardingMsConnector.onboardingUsersComplete(onboardingId, contract);
        log.debug("completeOnboardingUsersAsync result = success");
        log.trace("completeOnboardingUsersAsync end");
    }

    @Override
    public BinaryData getContract(String onboardingId) {
        log.trace("getContract start");
        log.debug("getContract id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        BinaryData resource = documentMsClient.getContract(onboardingId);
        log.debug("getContract result = success");
        log.trace("getContract end");
        return resource;
    }

    @Override
    public BinaryData getContractSigned(String onboardingId) {
        log.trace("getContractSigned start");
        log.debug("getContractSigned id = {}", Encode.forJava(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        BinaryData resource = documentMsClient.getContractSigned(onboardingId);
        log.debug("getContractSigned result = success");
        log.trace("getContractSigned end");
        return resource;
    }

    @Override
    public BinaryData getTemplateAttachment(String onboardingId, String filename) {
        log.trace("getTemplateAttachment start");
        log.debug("getTemplateAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");

        OnboardingData onboarding = onboardingMapper.toOnboardingData(onboardingMsConnector.getOnboarding(onboardingId));
        Product product = productService.getProductValid(onboarding.getProductId());
        String templatePath = getAttachmentTemplate(filename, onboarding, product).getTemplatePath();

        BinaryData resource = documentMsClient.getTemplateAttachment(
                onboarding.getId(),
                onboarding.getInstitutionUpdate().getDescription(),
                filename,
                onboarding.getProductId(),
                templatePath);
        log.debug("getTemplateAttachment result = success");
        log.trace("getTemplateAttachment end");
        return resource;
    }

    @Override
    public BinaryData getAttachment(String onboardingId, String filename) {
        log.trace("getAttachment start");
        log.debug("getAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");
        BinaryData resource = documentMsClient.getAttachment(onboardingId, filename);
        log.debug("getAttachment result = success");
        log.trace("getAttachment end");
        return resource;
    }

    @Override
    public AvailableDocuments getAvailableDocuments(String onboardingId) {
        log.trace("getAvailableDocuments start");
        log.debug("getAvailableDocuments onboardingId = {}", Encode.forJava(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        AvailableDocuments result = documentMsClient.getAvailableDocuments(onboardingId);
        log.debug("getAvailableDocuments result = success");
        log.trace("getAvailableDocuments end");
        return result;
    }

    @Override
    public BinaryData getAggregatesCsv(String onboardingId, String productId) {
        log.trace("getAggregatesCsv start");
        log.debug("getAggregatesCsv id = {}, productId = {}", Encode.forJava(onboardingId), Encode.forJava(productId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        Objects.requireNonNull(productId, "ProductId is required");
        BinaryData resource = documentMsClient.getAggregatesCsv(onboardingId, productId);
        log.debug("getAggregatesCsv result = success");
        log.trace("getAggregatesCsv end");
        return resource;
    }

    @Override
    public void uploadAttachment(String tenantId, String onboardingId, UploadedFile attachment, String attachmentName,
                                 String attachmentId, String attachmentDescription) {
        log.trace("uploadAttachment start");
        log.debug("uploadAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(attachmentName));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(attachmentName, "filename is required");
        Objects.requireNonNull(attachment, "file is required");
        OnboardingData onboarding = onboardingMapper.toOnboardingData(onboardingMsConnector.getOnboarding(onboardingId));

        Optional<RequiredDocumentModel> requiredDocument = findRequiredDocument(tenantId, onboarding, attachmentId);
        boolean userStorage = requiredDocument
                .map(RequiredDocumentModel::getStorageOrigin)
                .map(storageOrigin -> storageOrigin == StorageOrigin.USER)
                .orElse(false);

        if (userStorage) {
            Integer maxDocumentsRequired = requiredDocument
                    .map(RequiredDocumentModel::getMaxDocumentsRequired)
                    .orElse(1);
            log.info("Upload attachment {} for onboardingId {} on user storage", Encode.forJava(attachmentName), Encode.forJava(onboardingId));
            documentMsClient.uploadUserAttachment(
                    onboardingId,
                    attachment,
                    onboarding.getProductId(),
                    attachmentId,
                    attachmentDescription,
                    attachmentName,
                    maxDocumentsRequired);
        } else {
            Product product = productService.getProductValid(onboarding.getProductId());
            AttachmentTemplate template = getAttachmentTemplate(attachmentName, onboarding, product);
            log.info("Upload attachment {} for onboardingId {} on system storage", Encode.forJava(attachmentName), Encode.forJava(onboardingId));
            documentMsClient.uploadAttachment(onboardingId, attachment, attachmentName, product.getId(), template);
        }
        log.debug("uploadAttachment result = success");
        log.trace("uploadAttachment end");
    }

    @Override
    public int headAttachment(String onboardingId, String filename) {
        log.trace("headAttachment start");
        log.debug("headAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");
        int resource = documentMsClient.headAttachment(onboardingId, filename);
        log.debug("headAttachment result {}", resource);
        log.trace("headAttachment end");
        return resource;
    }

    private Optional<RequiredDocumentModel> findRequiredDocument(String tenantId, OnboardingData onboarding, String attachmentId) {
        return productService
                .getRequiredDocuments(
                        tenantId,
                        onboarding.getProductId(),
                        onboarding.getInstitutionType().name(),
                        onboarding.getInstitutionUpdate().getOrigin())
                .stream()
                .filter(requiredDocumentModel -> requiredDocumentModel.getId().equals(attachmentId))
                .findFirst();
    }

    private AttachmentTemplate getAttachmentTemplate(String attachmentName, OnboardingData onboarding, Product product) {
        return product
                .getInstitutionContractMappings()
                .get(onboarding.getInstitutionType().name())
                .getAttachments()
                .stream()
                .filter(a -> a.getName().equals(attachmentName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Attachment with name %s not found", attachmentName)));
    }
}
