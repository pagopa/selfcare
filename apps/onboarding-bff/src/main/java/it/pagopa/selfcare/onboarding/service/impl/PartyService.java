package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.client.PartyProcessRestClient;
import it.pagopa.selfcare.onboarding.client.model.BillingDataResponse;
import it.pagopa.selfcare.onboarding.client.model.Institution;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfo;
import it.pagopa.selfcare.onboarding.client.model.InstitutionResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionSeed;
import it.pagopa.selfcare.onboarding.client.model.InstitutionsResponse;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResource;
import it.pagopa.selfcare.onboarding.client.model.OnboardingsResponse;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.util.Preconditions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.onboarding_json.api.InstitutionControllerApi;
import org.openapi.quarkus.user_json.api.UserControllerApi;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Institution operations on party-process, user-ms and ms-onboarding.
 */
@ApplicationScoped
@Slf4j
public class PartyService {

    protected static final String REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE = "An Institution external id is required";
    protected static final String REQUIRED_INSTITUTION_ID_MESSAGE = "An Institution id is required";
    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product Id is required";
    protected static final String REQUIRED_INSTITUTION_TAXCODE_MESSAGE = "An Institution tax code is required";
    private static final String ACTIVE = "ACTIVE";
    private static final int USER_INSTITUTIONS_PAGE_SIZE = 500;

    private final PartyProcessRestClient restClient;
    private final InstitutionMapper institutionMapper;
    private final UserControllerApi userApiClient;
    private final InstitutionControllerApi institutionApiClient;

    public PartyService(@RestClient PartyProcessRestClient restClient,
                        InstitutionMapper institutionMapper,
                        @RestClient UserControllerApi userApiClient,
                        @RestClient InstitutionControllerApi institutionApiClient) {
        this.restClient = restClient;
        this.institutionMapper = institutionMapper;
        this.userApiClient = userApiClient;
        this.institutionApiClient = institutionApiClient;
    }

    public void onboardingOrganization(OnboardingData onboardingData) {
        Preconditions.notNull(onboardingData, "Onboarding data is required");
        restClient.onboardingOrganization(institutionMapper.toOnboardingInstitutionRequest(onboardingData));
    }

    /**
     * The institutions of the user for the product. When the product has a parent, the institutions onboarded
     * on the parent product that are not yet onboarded on the product itself are returned.
     */
    public List<InstitutionInfo> getInstitutionsByUser(Product product, String userId) {
        log.trace("getInstitutionsByUser start");
        List<UserInstitutionResponse> userInstitutions = findActiveUserInstitutions(product.getId(), userId);

        List<InstitutionInfo> result;
        if (Objects.nonNull(product.getParentId())) {
            List<UserInstitutionResponse> parentUserInstitutions = findActiveUserInstitutions(product.getParentId(), userId);
            List<String> childInstitutionIds = userInstitutions.stream()
                    .map(UserInstitutionResponse::getInstitutionId)
                    .toList();
            result = parentUserInstitutions.stream()
                    .filter(parentInstitution -> !childInstitutionIds.contains(parentInstitution.getInstitutionId()))
                    .map(institutionMapper::toInstitutionInfo)
                    .toList();
        } else {
            result = Objects.requireNonNull(userInstitutions).stream()
                    .map(institutionMapper::toInstitutionInfo)
                    .toList();
        }

        Map<String, org.openapi.quarkus.onboarding_json.model.InstitutionResponse> institutionsById = buildInstitutionMap(result);

        List<String> allowedTypes = product.getInstitutionTypesAllowed();
        List<InstitutionInfo> allowedInstitutions = Objects.isNull(allowedTypes) || allowedTypes.isEmpty()
                ? result
                : result.stream()
                .filter(institutionInfo -> institutionsById.containsKey(institutionInfo.getId())
                        && allowedTypes.contains(institutionsById.get(institutionInfo.getId()).getInstitutionType()))
                .toList();
        log.trace("getInstitutionsByUser end");
        return allowedInstitutions;
    }

    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public List<Institution> getInstitutionsByTaxCodeAndSubunitCode(String taxCode, String subunitCode) {
        Preconditions.hasText(taxCode, REQUIRED_INSTITUTION_TAXCODE_MESSAGE);
        InstitutionsResponse response = restClient.getInstitutions(taxCode, subunitCode);
        return response.getInstitutions().stream()
                .map(institutionMapper::toEntity)
                .toList();
    }

    public Institution getInstitutionByExternalId(String externalInstitutionId) {
        Preconditions.hasText(externalInstitutionId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);
        InstitutionResponse response = restClient.getInstitutionByExternalId(externalInstitutionId);
        return institutionMapper.toEntity(response);
    }

    public Institution getInstitutionById(String institutionId, String productId) {
        Preconditions.hasText(institutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        InstitutionResponse response = restClient.getInstitutionById(institutionId, productId);
        return institutionMapper.toEntity(response);
    }

    public List<OnboardingResource> getOnboardings(String institutionId, String productId) {
        Preconditions.hasText(institutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        OnboardingsResponse onboardings = restClient.getOnboardings(institutionId, productId);
        return onboardings.getOnboardings().stream()
                .map(institutionMapper::toResource)
                .toList();
    }

    public Institution createInstitutionFromIpa(String taxCode, String subunitCode, String subunitType) {
        Preconditions.hasText(taxCode, REQUIRED_INSTITUTION_TAXCODE_MESSAGE);
        return institutionMapper.toEntity(restClient.createInstitutionFromIpa(
                institutionMapper.toInstitutionFromIpaPost(taxCode, subunitCode, subunitType)));
    }

    public Institution createInstitutionFromANAC(OnboardingData onboardingData) {
        Preconditions.notNull(onboardingData, "An OnboardingData is required");
        return institutionMapper.toEntity(restClient.createInstitutionFromANAC(new InstitutionSeed(onboardingData)));
    }

    public Institution createInstitutionFromIVASS(OnboardingData onboardingData) {
        Preconditions.notNull(onboardingData, "An OnboardingData is required");
        return institutionMapper.toEntity(restClient.createInstitutionFromIVASS(new InstitutionSeed(onboardingData)));
    }

    public Institution createInstitutionFromInfocamere(OnboardingData onboardingData) {
        Preconditions.notNull(onboardingData, "An OnboardingData is required");
        return institutionMapper.toEntity(restClient.createInstitutionFromInfocamere(new InstitutionSeed(onboardingData)));
    }

    public Institution createInstitution(OnboardingData onboardingData) {
        Preconditions.notNull(onboardingData, "An OnboardingData is required");
        return institutionMapper.toEntity(restClient.createInstitution(new InstitutionSeed(onboardingData)));
    }

    public InstitutionInfo getInstitutionBillingData(String externalInstitutionId, String productId) {
        Preconditions.hasText(externalInstitutionId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);
        Preconditions.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        BillingDataResponse response = restClient.getInstitutionBillingData(externalInstitutionId, productId);
        return institutionMapper.toInstitutionInfo(response);
    }

    public void verifyOnboarding(String externalInstitutionId, String productId) {
        Preconditions.hasText(externalInstitutionId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);
        Preconditions.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        restClient.verifyOnboarding(externalInstitutionId, productId);
    }

    public void verifyOnboarding(String productId, String externalId, String taxCode, String origin, String originId, String subunitCode) {
        Preconditions.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        restClient.verifyOnboardingInfoByFilters(productId, externalId, taxCode, origin, originId, subunitCode);
    }

    private List<UserInstitutionResponse> findActiveUserInstitutions(String productId, String userId) {
        List<String> products = Objects.isNull(productId) ? null : List.of(productId);
        return userApiClient.usersGet(null, null, null, products, null, USER_INSTITUTIONS_PAGE_SIZE, List.of(ACTIVE), userId)
                .await().indefinitely();
    }

    private Map<String, org.openapi.quarkus.onboarding_json.model.InstitutionResponse> buildInstitutionMap(List<InstitutionInfo> result) {
        if (result.isEmpty()) {
            return Map.of();
        }
        List<org.openapi.quarkus.onboarding_json.model.InstitutionResponse> response =
                institutionApiClient.getInstitutions(institutionMapper.toGetInstitutionRequest(result)).await().indefinitely();
        return Objects.isNull(response)
                ? Map.of()
                : response.stream().collect(Collectors.toMap(
                        org.openapi.quarkus.onboarding_json.model.InstitutionResponse::getId, Function.identity()));
    }
}
