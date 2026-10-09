package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
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
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.dto.request.DownloadDocumentType;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.apache.commons.lang3.StringUtils;

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
    public Uni<OnboardingData> verifyOnboarding(String onboardingId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding id = {}", onboardingId);
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        return onboardingMsConnector.getOnboarding(onboardingId)
                .map(onboardingMapper::toOnboardingData)
                .invoke(data -> {
                    log.debug("verifyOnboarding result = success");
                    log.trace("verifyOnboarding end");
                });
    }

    @Override
    public Uni<Void> approveOnboarding(String onboardingId, String userUid) {
        log.trace("approveOnboarding start");
        log.debug("approveOnboarding id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        return onboardingMsConnector.approveOnboarding(onboardingId, userUid)
                .invoke(() -> {
                    log.debug("approveOnboarding result = success");
                    log.trace("approveOnboarding end");
                });
    }

    @Override
    public Uni<Void> rejectOnboarding(String onboardingId, String reason, String userUid) {
        log.trace("rejectOnboarding start");
        log.debug("rejectOnboarding id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        return onboardingMsConnector.rejectOnboarding(onboardingId, reason, userUid)
                .invoke(() -> {
                    log.debug("rejectOnboarding result = success");
                    log.trace("rejectOnboarding end");
                });
    }

    @Override
    public Uni<OnboardingData> getOnboardingWithUserInfo(String onboardingId) {
        log.trace("getOnboardingWithUserInfo start");
        log.debug("getOnboardingWithUserInfo id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        return onboardingMsConnector.getOnboardingWithUserInfo(onboardingId)
                .map(onboardingMapper::toOnboardingData)
                .invoke(onboardingData -> {
                    log.debug("getOnboardingWithUserInfo result = success");
                    log.trace("getOnboardingWithUserInfo end");
                });
    }

    @Override
    public Uni<Void> completeTokenV2(String onboardingId, UploadedFile contract) {
        log.trace("completeTokenAsync start");
        log.debug("completeTokenAsync id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        return onboardingMsConnector.onboardingTokenComplete(onboardingId, contract)
                .invoke(() -> {
                    log.debug("completeTokenAsync result = success");
                    log.trace("completeTokenAsync end");
                });
    }

    @Override
    public Uni<Void> completeOnboardingUsers(String onboardingId, UploadedFile contract) {
        log.trace("completeOnboardingUsersAsync start");
        log.debug("completeOnboardingUsersAsync id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        return onboardingMsConnector.onboardingUsersComplete(onboardingId, contract)
                .invoke(() -> {
                    log.debug("completeOnboardingUsersAsync result = success");
                    log.trace("completeOnboardingUsersAsync end");
                });
    }

    @Override
    public Uni<BinaryData> getContract(String onboardingId) {
        log.trace("getContract start");
        log.debug("getContract id = {}", LogUtils.sanitize(onboardingId));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        return documentMsClient.getContract(onboardingId)
                .invoke(resource -> {
                    log.debug("getContract result = success");
                    log.trace("getContract end");
                });
    }

    @Override
    public Uni<BinaryData> getContractSigned(String onboardingId) {
        log.trace("getContractSigned start");
        log.debug("getContractSigned id = {}", Encode.forJava(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        return documentMsClient.getContractSigned(onboardingId)
                .invoke(resource -> {
                    log.debug("getContractSigned result = success");
                    log.trace("getContractSigned end");
                });
    }

    @Override
    public Uni<BinaryData> getTemplateAttachment(String onboardingId, String filename) {
        log.trace("getTemplateAttachment start");
        log.debug("getTemplateAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");

        return onboardingMsConnector.getOnboarding(onboardingId)
                .map(onboardingMapper::toOnboardingData)
                .chain(onboarding -> productService.getProductValid(onboarding.getProductId())
                        .chain(product -> documentMsClient.getTemplateAttachment(
                                onboarding.getId(), onboarding.getInstitutionUpdate().getDescription(), filename,
                                onboarding.getProductId(), getAttachmentTemplate(filename, onboarding, product).getTemplatePath())))
                .invoke(resource -> {
                    log.debug("getTemplateAttachment result = success");
                    log.trace("getTemplateAttachment end");
                });
    }

    @Override
    public Uni<BinaryData> getAttachment(String onboardingId, String filename) {
        log.trace("getAttachment start");
        log.debug("getAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");
        return documentMsClient.getAttachment(onboardingId, filename)
                .invoke(resource -> {
                    log.debug("getAttachment result = success");
                    log.trace("getAttachment end");
                });
    }

    @Override
    public Uni<BinaryData> downloadDocument(String onboardingId, DownloadDocumentType type, String filename) {
        return switch (type) {
            case CONTRACT_SIGNED -> getContractSigned(onboardingId);
            case ATTACHMENT -> {
                if (StringUtils.isBlank(filename)) {
                    throw new InvalidRequestException("Query parameter 'name' is required when type=" + type);
                }
                yield getAttachment(onboardingId, filename);
            }
            default -> throw new InvalidRequestException("Unsupported download type: " + type);
        };
    }

    @Override
    public Uni<AvailableDocuments> getAvailableDocuments(String onboardingId) {
        log.trace("getAvailableDocuments start");
        log.debug("getAvailableDocuments onboardingId = {}", Encode.forJava(onboardingId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        return documentMsClient.getAvailableDocuments(onboardingId)
                .invoke(result -> {
                    log.debug("getAvailableDocuments result = success");
                    log.trace("getAvailableDocuments end");
                });
    }

    @Override
    public Uni<BinaryData> getAggregatesCsv(String onboardingId, String productId) {
        log.trace("getAggregatesCsv start");
        log.debug("getAggregatesCsv id = {}, productId = {}", Encode.forJava(onboardingId), Encode.forJava(productId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        Objects.requireNonNull(productId, "ProductId is required");
        return documentMsClient.getAggregatesCsv(onboardingId, productId)
                .invoke(resource -> {
                    log.debug("getAggregatesCsv result = success");
                    log.trace("getAggregatesCsv end");
                });
    }

    @Override
    public Uni<Void> uploadAttachment(String tenantId, String onboardingId, UploadedFile attachment, String attachmentName,
                                 String attachmentId, String attachmentDescription) {
        log.trace("uploadAttachment start");
        log.debug("uploadAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(attachmentName));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(attachmentName, "filename is required");
        Objects.requireNonNull(attachment, "file is required");
        return onboardingMsConnector.getOnboarding(onboardingId)
                .map(onboardingMapper::toOnboardingData)
                .chain(onboarding -> findRequiredDocument(tenantId, onboarding, attachmentId)
                        .chain(requiredDocument -> {
                            boolean userStorage = requiredDocument.map(RequiredDocumentModel::getStorageOrigin)
                                    .map(storageOrigin -> storageOrigin == StorageOrigin.USER).orElse(false);
                            if (userStorage) {
                                Integer maxDocumentsRequired = requiredDocument.map(RequiredDocumentModel::getMaxDocumentsRequired).orElse(1);
                                log.info("Upload attachment {} for onboardingId {} on user storage", Encode.forJava(attachmentName), Encode.forJava(onboardingId));
                                return documentMsClient.uploadUserAttachment(onboardingId, attachment, onboarding.getProductId(),
                                        attachmentId, attachmentDescription, attachmentName, maxDocumentsRequired);
                            }
                            return productService.getProductValid(onboarding.getProductId())
                                    .chain(product -> {
                                        AttachmentTemplate template = getAttachmentTemplate(attachmentName, onboarding, product);
                                        log.info("Upload attachment {} for onboardingId {} on system storage", Encode.forJava(attachmentName), Encode.forJava(onboardingId));
                                        return documentMsClient.uploadAttachment(onboardingId, attachment, attachmentName, product.getId(), template);
                                    });
                        }))
                .invoke(() -> {
                    log.debug("uploadAttachment result = success");
                    log.trace("uploadAttachment end");
                });
    }

    @Override
    public Uni<Integer> headAttachment(String onboardingId, String filename) {
        log.trace("headAttachment start");
        log.debug("headAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");
        return documentMsClient.headAttachment(onboardingId, filename)
                .invoke(resource -> {
                    log.debug("headAttachment result {}", resource);
                    log.trace("headAttachment end");
                });
    }

    private Uni<Optional<RequiredDocumentModel>> findRequiredDocument(String tenantId, OnboardingData onboarding, String attachmentId) {
        return productService
                .getRequiredDocuments(
                        tenantId,
                        onboarding.getProductId(),
                        onboarding.getInstitutionType().name(),
                        onboarding.getInstitutionUpdate().getOrigin())
                .map(documents -> documents.stream()
                        .filter(requiredDocumentModel -> requiredDocumentModel.getId().equals(attachmentId))
                        .findFirst());
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
