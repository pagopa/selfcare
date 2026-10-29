package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.client.OnboardingRestClient;
import it.pagopa.selfcare.dashboard.model.document.DocumentStatusResource;
import it.pagopa.selfcare.dashboard.model.document.ProductDocumentStatusResource;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGet;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingStatus;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.WorkflowType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String REQUIRED_INSTITUTION_MESSAGE = "An Institution id is required";

    private final OnboardingRestClient onboardingRestClient;

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

}
