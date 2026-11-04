package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingsResponse;
import it.pagopa.selfcare.dashboard.client.CoreInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.client.DocumentContentRestClient;
import it.pagopa.selfcare.dashboard.client.DocumentRestClient;
import it.pagopa.selfcare.dashboard.client.OnboardingRestClient;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.model.document.DocumentResource;
import it.pagopa.selfcare.dashboard.model.document.DocumentStatusResource;
import it.pagopa.selfcare.dashboard.model.document.DocumentType;
import it.pagopa.selfcare.dashboard.model.document.DocumentsResource;
import it.pagopa.selfcare.dashboard.model.document.ProductDocumentStatusResource;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.AvailableDocumentResponse;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.AvailableDocumentsResponse;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGet;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingStatus;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.WorkflowType;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final String REQUIRED_INSTITUTION_MESSAGE = "An Institution id is required";
    private static final String REQUIRED_PRODUCT_MESSAGE = "A Product id is required";
    private static final String REQUIRED_TYPE_MESSAGE = "A document type is required";
    private static final String REQUIRED_NAME_MESSAGE = "A document name is required";
    private static final Pattern FILE_EXTENSIONS_PATTERN = Pattern.compile("\\.(pdf|p7m)(\\..*)?$", Pattern.CASE_INSENSITIVE);

    private final OnboardingRestClient onboardingRestClient;
    private final CoreInstitutionApiRestClient coreInstitutionApiRestClient;
    private final DocumentRestClient documentRestClient;
    private final DocumentContentRestClient documentContentRestClient;
    private final List<String> allowedAttachments;

    public DocumentServiceImpl(OnboardingRestClient onboardingRestClient,
                               CoreInstitutionApiRestClient coreInstitutionApiRestClient,
                               DocumentRestClient documentRestClient,
                               DocumentContentRestClient documentContentRestClient,
                               @Value("${dashboard.documents.allowed-attachments}") List<String> allowedAttachments) {
        this.onboardingRestClient = onboardingRestClient;
        this.coreInstitutionApiRestClient = coreInstitutionApiRestClient;
        this.documentRestClient = documentRestClient;
        this.documentContentRestClient = documentContentRestClient;
        this.allowedAttachments = Optional.ofNullable(allowedAttachments).orElse(Collections.emptyList());
    }

    @Override
    public DocumentStatusResource getDocumentStatus(String institutionId, List<String> productIds) {
        log.trace("getDocumentStatus start");
        log.debug("getDocumentStatus institutionId = {}, productIds = {}", Encode.forJava(institutionId), Encode.forJava(String.valueOf(productIds)));
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_MESSAGE);

        final List<String> requestedProductIds = Optional.ofNullable(productIds).orElse(Collections.emptyList()).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (requestedProductIds.isEmpty()) {
            log.trace("getDocumentStatus end");
            return new DocumentStatusResource(Collections.emptyList());
        }

        final List<OnboardingGet> onboardings = Optional.ofNullable(onboardingRestClient._getOnboardingWithFilter(
                        null, institutionId, null, null, null, requestedProductIds, null, true, null, null, null, null, null, null))
                .map(ResponseEntity::getBody)
                .map(OnboardingGetResponse::getItems)
                .orElse(Collections.emptyList());
        final LocalDateTime now = LocalDateTime.now();

        final List<ProductDocumentStatusResource> products = requestedProductIds.stream()
                .map(productId -> toProductDocumentStatus(productId, onboardings, now))
                .toList();

        log.trace("getDocumentStatus end");
        return new DocumentStatusResource(products);
    }

    private static ProductDocumentStatusResource toProductDocumentStatus(String productId, List<OnboardingGet> onboardings, LocalDateTime now) {
        final List<OnboardingGet> productOnboardings = onboardings.stream()
                .filter(onboarding -> productId.equals(onboarding.getProductId()))
                .toList();
        final boolean contractUpdateRequired = productOnboardings.stream()
                .anyMatch(onboarding -> OnboardingStatus.COMPLETED.getValue().equals(onboarding.getStatus())
                        && Boolean.TRUE.equals(onboarding.getContractUpdateRequired()));
        final boolean contractUpdateInProgress = productOnboardings.stream()
                .anyMatch(onboarding -> isActiveContractUpdate(onboarding, now));
        return new ProductDocumentStatusResource(productId, contractUpdateRequired, contractUpdateInProgress);
    }

    // Expiration is lazy: an expired request stays PENDING on onboarding-ms but is no longer active
    private static boolean isActiveContractUpdate(OnboardingGet onboarding, LocalDateTime now) {
        return WorkflowType.CONTRACT_UPDATE.getValue().equals(onboarding.getWorkflowType())
                && OnboardingStatus.PENDING.getValue().equals(onboarding.getStatus())
                && (Objects.isNull(onboarding.getExpiringDate()) || now.isBefore(onboarding.getExpiringDate()));
    }

    @Override
    public DocumentsResource getDocumentsList(String institutionId, String productId) {
        log.trace("getDocumentsList start");
        log.debug("getDocumentsList institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_MESSAGE);
        Assert.hasText(productId, REQUIRED_PRODUCT_MESSAGE);

        final Optional<String> activeTokenId = getActiveOnboarding(institutionId, productId).map(OnboardingResponse::getTokenId);
        if (activeTokenId.isEmpty()) {
            log.warn("getDocumentsList no active onboarding found for institutionId = {}, productId = {}",
                    Encode.forJava(institutionId), Encode.forJava(productId));
            return new DocumentsResource(Collections.emptyList());
        }

        final String tokenId = activeTokenId.get();
        final List<AvailableDocumentResponse> availableDocuments = Optional.ofNullable(documentRestClient._getAvailableDocuments(tokenId))
                .map(ResponseEntity::getBody)
                .map(AvailableDocumentsResponse::getDocuments)
                .orElse(Collections.emptyList());
        final Optional<AvailableDocumentResponse> currentContract = getLatestCompletedContract(availableDocuments, tokenId, institutionId, productId);

        final List<DocumentResource> documents = availableDocuments.stream()
                .filter(document -> !isInstitutionContract(document) || currentContract.filter(contract -> contract == document).isPresent())
                .map(this::toDocumentResource)
                .flatMap(Optional::stream)
                .toList();

        log.trace("getDocumentsList end");
        return new DocumentsResource(documents);
    }

    /**
     * The dossier contains the contract of the institution adhesion and the ones of its contract updates (ordered by creation date):
     * only the contract of the most recent completed onboarding is exposed. The active onboarding of institution-ms is completed by
     * definition, the others are checked on onboarding-ms only when needed.
     */
    private Optional<AvailableDocumentResponse> getLatestCompletedContract(List<AvailableDocumentResponse> documents, String tokenId,
                                                                           String institutionId, String productId) {
        final List<AvailableDocumentResponse> contracts = documents.stream()
                .filter(DocumentServiceImpl::isInstitutionContract)
                .toList();
        final Set<String> completedOnboardingIds = new HashSet<>(Set.of(tokenId));
        if (contracts.stream().anyMatch(contract -> !tokenId.equals(contract.getOnboardingId()))) {
            completedOnboardingIds.addAll(getCompletedOnboardingIds(institutionId, productId));
        }
        return contracts.stream()
                .filter(contract -> completedOnboardingIds.contains(contract.getOnboardingId()))
                .reduce((previous, next) -> next);
    }

    private Set<String> getCompletedOnboardingIds(String institutionId, String productId) {
        return Optional.ofNullable(onboardingRestClient._getOnboardingWithFilter(
                        null, institutionId, null, null, null, List.of(productId), null, true, null, null, null, null, null, null))
                .map(ResponseEntity::getBody)
                .map(OnboardingGetResponse::getItems)
                .orElse(Collections.emptyList())
                .stream()
                .filter(onboarding -> OnboardingStatus.COMPLETED.getValue().equals(onboarding.getStatus()))
                .map(OnboardingGet::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static boolean isInstitutionContract(AvailableDocumentResponse document) {
        return it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentType.INSTITUTION.equals(document.getType());
    }

    @Override
    public Resource getDocument(String institutionId, String productId, String name, DocumentType type) {
        log.trace("getDocument start");
        log.debug("getDocument institutionId = {}, productId = {}, name = {}, type = {}",
                Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(name), type);
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_MESSAGE);
        Assert.hasText(productId, REQUIRED_PRODUCT_MESSAGE);
        Assert.notNull(type, REQUIRED_TYPE_MESSAGE);
        // The name identifies the attachment only: the contract is resolved by the onboarding, so the name is optional for it
        if (DocumentType.ATTACHMENT.equals(type) && !StringUtils.hasText(name)) {
            throw new ValidationException(REQUIRED_NAME_MESSAGE);
        }

        final OnboardingResponse onboarding = getActiveOnboarding(institutionId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active onboarding found for institution " + institutionId + " and product " + productId));

        final Resource document = switch (type) {
            case INSTITUTION -> documentContentRestClient._getContractSigned(onboarding.getTokenId(), null).getBody();
            case ATTACHMENT -> {
                if (!allowedAttachments.contains(name)) {
                    throw new ResourceNotFoundException("Attachment " + name + " not available for download");
                }
                yield documentContentRestClient._getAttachment(onboarding.getTokenId(), name).getBody();
            }
        };

        log.trace("getDocument end");
        return document;
    }

    // The onboarding is read from institution-ms so that the document dossier always belongs to the institution and product checked by the permission
    private Optional<OnboardingResponse> getActiveOnboarding(String institutionId, String productId) {
        return Optional.ofNullable(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .map(ResponseEntity::getBody)
                .map(OnboardingsResponse::getOnboardings)
                .orElse(Collections.emptyList())
                .stream()
                .filter(onboarding -> productId.equals(onboarding.getProductId()))
                .filter(onboarding -> OnboardingResponse.StatusEnum.ACTIVE.equals(onboarding.getStatus()))
                .filter(onboarding -> Objects.nonNull(onboarding.getTokenId()))
                .max(Comparator.comparing(OnboardingResponse::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    // Only institution contracts and the allowed attachments (e.g. DORA addendum) are exposed: USER contracts are excluded
    private Optional<DocumentResource> toDocumentResource(AvailableDocumentResponse document) {
        if (Objects.isNull(document.getType())) {
            return Optional.empty();
        }
        return switch (document.getType()) {
            case INSTITUTION -> Optional.of(new DocumentResource(toDisplayName(document.getName()), DocumentType.INSTITUTION));
            case ATTACHMENT -> allowedAttachments.contains(document.getName())
                    ? Optional.of(new DocumentResource(document.getName(), DocumentType.ATTACHMENT))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    // Strips the file extensions from the first ".pdf"/".p7m" onwards: dots inside the name (e.g. "v. 2.3") are kept,
    static String toDisplayName(String fileName) {
        if (Objects.isNull(fileName)) {
            return null;
        }
        return FILE_EXTENSIONS_PATTERN.matcher(fileName).replaceFirst("").trim();
    }

}
