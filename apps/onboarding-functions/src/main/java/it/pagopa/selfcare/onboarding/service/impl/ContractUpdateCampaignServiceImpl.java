package it.pagopa.selfcare.onboarding.service.impl;

import static it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignResponse.Outcome.*;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignRequest;
import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignResponse;
import it.pagopa.selfcare.onboarding.dto.NotificationMailRequest;
import it.pagopa.selfcare.onboarding.dto.NotificationMailType;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.ContractUpdateCampaignService;
import it.pagopa.selfcare.onboarding.service.NotificationService;
import it.pagopa.selfcare.onboarding.service.OnboardingRepositoryService;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;

@Slf4j
@ApplicationScoped
public class ContractUpdateCampaignServiceImpl implements ContractUpdateCampaignService {

    static final String DOCUMENTS_URL_PLACEHOLDER = "documentsURL";

    private final OnboardingRepositoryService onboardingRepositoryService;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final MailTemplatePathConfig templatePathConfig;
    private final MailTemplatePlaceholdersConfig templatePlaceholdersConfig;

    @RestClient
    @Inject
    InstitutionApi institutionApi;

    public ContractUpdateCampaignServiceImpl(OnboardingRepositoryService onboardingRepositoryService,
                                             ProductService productService,
                                             NotificationService notificationService,
                                             MailTemplatePathConfig templatePathConfig,
                                             MailTemplatePlaceholdersConfig templatePlaceholdersConfig) {
        this.onboardingRepositoryService = onboardingRepositoryService;
        this.productService = productService;
        this.notificationService = notificationService;
        this.templatePathConfig = templatePathConfig;
        this.templatePlaceholdersConfig = templatePlaceholdersConfig;
    }

    @Override
    public ContractUpdateCampaignResponse send(ContractUpdateCampaignRequest request) {
        String institutionId = request.institutionId();

        List<Onboarding> adhesions = onboardingRepositoryService.findByQuery(
                new Document("institution.id", institutionId)
                        .append("productId", new Document("$in", request.productIds()))
                        .append("status", OnboardingStatus.COMPLETED.name())
                        .append("referenceOnboardingId", new Document("$exists", false)));

        if (!adhesions.stream().map(Onboarding::getProductId).collect(Collectors.toSet()).containsAll(request.productIds())) {
            log.warn("Missing COMPLETED adhesion - institutionId: {}, productIds: {}", institutionId, request.productIds());
            return new ContractUpdateCampaignResponse(INVALID, null);
        }

        // contractUpdateRequiredAt survives the end of the update, unlike the flag
        List<Onboarding> toNotify = adhesions.stream()
                .filter(onboarding -> Objects.isNull(onboarding.getContractUpdateRequiredAt()))
                .toList();
        if (toNotify.isEmpty()) {
            log.info("Institution already notified - institutionId: {}", institutionId);
            return new ContractUpdateCampaignResponse(ALREADY_NOTIFIED, null);
        }

        String digitalAddress = institutionApi.retrieveInstitutionByIdUsingGET(institutionId, null).getDigitalAddress();
        if (StringUtils.isBlank(digitalAddress)) {
            log.warn("Missing PEC - institutionId: {}", institutionId);
            return new ContractUpdateCampaignResponse(MISSING_PEC, null);
        }

        String productNames = toNotify.stream()
                .map(Onboarding::getProductId)
                .distinct()
                .map(productId -> productService.getProduct(productId).getTitle())
                .collect(Collectors.joining(" e "));

        notificationService.sendMail(NotificationMailRequest.builder()
                .type(NotificationMailType.CONTRACT_UPDATE_CAMPAIGN)
                .destinationMails(List.of(digitalAddress))
                .templatePath(templatePathConfig.contractUpdateCampaignPath())
                .mailParameters(Map.of(
                        templatePlaceholdersConfig.productName(), productNames,
                        DOCUMENTS_URL_PLACEHOLDER, templatePlaceholdersConfig.completeSelfcarePlaceholder()
                                + "/dashboard/" + institutionId + "/documents"))
                .build());

        onboardingRepositoryService.setContractUpdateRequired(
                toNotify.stream().map(Onboarding::getId).toList(), LocalDateTime.now());
        log.info("Contract update campaign PEC sent - institutionId: {}, onboardings: {}", institutionId, toNotify.size());
        return new ContractUpdateCampaignResponse(SENT, digitalAddress);
    }
}
