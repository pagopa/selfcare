package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.PartyProcessRestClient;
import it.pagopa.selfcare.onboarding.client.model.*;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import it.pagopa.selfcare.onboarding.exception.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import java.io.IOException;
import jakarta.ws.rs.ProcessingException;
import java.time.temporal.ChronoUnit;
import org.openapi.quarkus.onboarding_json.api.InstitutionControllerApi;
import org.openapi.quarkus.onboarding_json.model.GetInstitutionRequest;
import org.openapi.quarkus.user_json.api.UserControllerApi;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.client.model.RelationshipState.ACTIVE;

@ApplicationScoped
@Slf4j
public class PartyService {


    private final PartyProcessRestClient restClient;
    private final InstitutionMapper institutionMapper;
    private final UserControllerApi userApiClient;
    private final InstitutionControllerApi institutionApiClient;

    private final Function<RelationshipInfo, UserInfo> relationshipInfoToUserInfo = relationshipInfo -> {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(relationshipInfo.getFrom());
        userInfo.setRole(relationshipInfo.getRole());
        userInfo.setStatus(relationshipInfo.getState().name());
        userInfo.setInstitutionId(relationshipInfo.getTo());
        return userInfo;
    };

    private Map<String, org.openapi.quarkus.onboarding_json.model.InstitutionResponse> buildInstitutionMap(List<InstitutionInfo> result) {
        GetInstitutionRequest request = new GetInstitutionRequest();
        request.setInstitutionIds(result.stream().map(InstitutionInfo::getId).toList());
        List<org.openapi.quarkus.onboarding_json.model.InstitutionResponse> response = institutionApiClient.getInstitutions(request).await().indefinitely();
        return Objects.isNull(response) ? Map.of() : response.stream().collect(Collectors.toMap(org.openapi.quarkus.onboarding_json.model.InstitutionResponse::getId, Function.identity()));
    }
    public PartyService(@RestClient PartyProcessRestClient restClient,
                              InstitutionMapper institutionMapper,
                              @RestClient UserControllerApi userApiClient,
                              @RestClient InstitutionControllerApi institutionApiClient) {
        this.restClient = restClient;
        this.institutionMapper = institutionMapper;
        this.userApiClient = userApiClient;
        this.institutionApiClient = institutionApiClient;
    }
    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public void onboardingOrganization(OnboardingData onboardingData) {
        java.util.Objects.requireNonNull(onboardingData, "Onboarding data is required");
        OnboardingInstitutionRequest onboardingInstitutionRequest = new OnboardingInstitutionRequest();
        onboardingInstitutionRequest.setInstitutionExternalId(onboardingData.getInstitutionExternalId());
        onboardingInstitutionRequest.setPricingPlan(onboardingData.getPricingPlan());
        onboardingInstitutionRequest.setBilling(onboardingData.getBilling());
        onboardingInstitutionRequest.setProductId(onboardingData.getProductId());
        onboardingInstitutionRequest.setProductName(onboardingData.getProductName());
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setInstitutionType(onboardingData.getInstitutionType());
        institutionUpdate.setAddress(onboardingData.getInstitutionUpdate().getAddress());
        institutionUpdate.setDescription(onboardingData.getInstitutionUpdate().getDescription());
        institutionUpdate.setDigitalAddress(onboardingData.getInstitutionUpdate().getDigitalAddress());
        institutionUpdate.setTaxCode(onboardingData.getInstitutionUpdate().getTaxCode());
        institutionUpdate.setZipCode(onboardingData.getInstitutionUpdate().getZipCode());
        institutionUpdate.setPaymentServiceProvider(onboardingData.getInstitutionUpdate().getPaymentServiceProvider());
        institutionUpdate.setDataProtectionOfficer(onboardingData.getInstitutionUpdate().getDataProtectionOfficer());
        if (onboardingData.getLocation() != null) {
            institutionUpdate.setCity(onboardingData.getLocation().getCity());
            institutionUpdate.setCounty(onboardingData.getLocation().getCounty());
            institutionUpdate.setCountry(onboardingData.getLocation().getCountry());
        }
        if (Objects.nonNull(onboardingData.getInstitutionUpdate()) && Objects.nonNull(onboardingData.getInstitutionUpdate().getGeographicTaxonomies())) {
            institutionUpdate.setGeographicTaxonomyCodes(onboardingData.getInstitutionUpdate().getGeographicTaxonomies().stream()
                    .map(GeographicTaxonomy::getCode).toList());
        }
        institutionUpdate.setRea(onboardingData.getInstitutionUpdate().getRea());
        institutionUpdate.setShareCapital(onboardingData.getInstitutionUpdate().getShareCapital());
        institutionUpdate.setBusinessRegisterPlace(onboardingData.getInstitutionUpdate().getBusinessRegisterPlace());
        institutionUpdate.setSupportEmail(onboardingData.getInstitutionUpdate().getSupportEmail());
        institutionUpdate.setSupportPhone(onboardingData.getInstitutionUpdate().getSupportPhone());
        institutionUpdate.setImported(onboardingData.getInstitutionUpdate().getImported());
        onboardingInstitutionRequest.setInstitutionUpdate(institutionUpdate);
        onboardingInstitutionRequest.setUsers(onboardingData.getUsers().stream()
                .map(user -> {
                    User userRequest = new User();
                    userRequest.setTaxCode(user.getTaxCode());
                    userRequest.setRole(user.getRole());
                    userRequest.setEmail(user.getEmail());
                    userRequest.setName(user.getName());
                    userRequest.setSurname(user.getSurname());
                    userRequest.setProductRole(user.getProductRole());
                    return userRequest;
                }).toList());
        restClient.onboardingOrganization(onboardingInstitutionRequest);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public OnboardingContract getOnboardingContract(String institutionId, String productId) {
        return restClient.getOnboardingContract(institutionId, productId);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public List<InstitutionInfo> getOnboardings(String userId) {
        log.trace("getOnboardings start");
        log.debug("getOnboardings userId = {}", userId);
        RelationshipsResponse response = restClient.getUserInstitutions(userId, null,
                EnumSet.of(ACTIVE),
                null, null, null);
        List<InstitutionInfo> result = Collections.emptyList();
        if (response != null) {
            result = response.stream()
                    .map(relationshipInfoToUserInfo)
                    .map(userInfo -> {
                        InstitutionInfo institutionInfo = new InstitutionInfo();
                        institutionInfo.setId(userInfo.getInstitutionId());
                        institutionInfo.setStatus(userInfo.getStatus());
                        return institutionInfo;
                    })
                    .collect(Collectors.groupingBy(InstitutionInfo::getId))
                    .values().stream()
                    .map(institutionInfos -> institutionInfos.get(0))
                    .toList();
        }
        log.debug("getOnboardings result = {}", result);
        log.trace("getOnboardings end");
        return result;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public List<InstitutionInfo> getInstitutionsByUser(Product product, String userId) {
        log.trace("getInstitutionsByUser start");
        log.debug("getInstitutionsByUser product = {}, userId = {}", product, userId);
        RelationshipsResponse response = restClient.getUserInstitutions(userId, null,
                EnumSet.of(ACTIVE),
                List.of(product.getId()),
                null, null);
        List<InstitutionInfo> result = Collections.emptyList();
        if (response != null) {
            result = response.stream()
                    .map(relationshipInfoToUserInfo)
                    .map(userInfo -> {
                        InstitutionInfo institutionInfo = new InstitutionInfo();
                        institutionInfo.setId(userInfo.getInstitutionId());
                        institutionInfo.setUserRole(userInfo.getRole());
                        institutionInfo.setStatus(userInfo.getStatus());
                        return institutionInfo;
                    })
                    .toList();
            if (!result.isEmpty()) {
                Map<String, org.openapi.quarkus.onboarding_json.model.InstitutionResponse> institutionMap = buildInstitutionMap(result);
                result.forEach(institutionInfo -> {
                    org.openapi.quarkus.onboarding_json.model.InstitutionResponse institutionResponse = institutionMap.get(institutionInfo.getId());
                    if (Objects.nonNull(institutionResponse)) {
                        institutionInfo.setDescription(institutionResponse.getDescription());
                        institutionInfo.setExternalId(institutionInfo.getId());
                        institutionInfo.setTaxCode(institutionResponse.getTaxCode());
                        institutionInfo.setOrigin(institutionResponse.getOrigin() != null ? institutionResponse.getOrigin().name() : null);
                        institutionInfo.setOriginId(institutionResponse.getOriginId());
                        institutionInfo.setDigitalAddress(institutionResponse.getDigitalAddress());
                        institutionInfo.setZipCode(institutionResponse.getZipCode());
                        institutionInfo.setAddress(institutionResponse.getAddress());
                        institutionInfo.setInstitutionType(it.pagopa.selfcare.onboarding.common.InstitutionType.valueOf(institutionResponse.getInstitutionType()));
                    }
                });
            }
        }
        log.debug("getInstitutionsByUser result = {}", result);
        log.trace("getInstitutionsByUser end");
        return result;
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public List<Institution> getInstitutionsByTaxCodeAndSubunitCode(String taxCode, String subunitCode) {
        return restClient.getInstitutionsByTaxCodeAndSubunitCode(taxCode, subunitCode);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public Institution getInstitutionByExternalId(String externalId) {
        return restClient.getInstitutionByExternalId(externalId);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public Institution getInstitutionById(String id) {
        return restClient.getInstitutionById(id);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public List<OnboardingResource> getOnboardings(String institutionId, String productId) {
        return restClient.getOnboardings(institutionId, productId);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public Institution createInstitutionFromIpa(String taxCode, String subunitCode, it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType subunitType) {
        InstitutionFromIpaPost institutionFromIpaPost = new InstitutionFromIpaPost();
        institutionFromIpaPost.setTaxCode(taxCode);
        institutionFromIpaPost.setSubunitCode(subunitCode);
        if (subunitType != null) {
            institutionFromIpaPost.setSubunitType(subunitType.name());
        }
        return restClient.createInstitutionFromIpa(institutionFromIpaPost);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public Institution createInstitution(OnboardingData onboardingData) {
        InstitutionSeed institutionSeed = institutionMapper.toInstitutionSeed(onboardingData);
        return restClient.createInstitution(institutionSeed);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public Institution createInstitutionFromANAC(OnboardingData onboardingData) {
        InstitutionSeed institutionSeed = institutionMapper.toInstitutionSeed(onboardingData);
        return restClient.createInstitutionFromANAC(institutionSeed);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public Institution createInstitutionFromIVASS(OnboardingData onboardingData) {
        InstitutionSeed institutionSeed = institutionMapper.toInstitutionSeed(onboardingData);
        return restClient.createInstitutionFromIVASS(institutionSeed);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public Institution createInstitutionFromInfocamere(OnboardingData onboardingData) {
        InstitutionSeed institutionSeed = institutionMapper.toInstitutionSeed(onboardingData);
        return restClient.createInstitutionFromInfocamere(institutionSeed);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public void verifyOnboarding(String externalId, String productId) {
        restClient.verifyOnboarding(externalId, productId);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public void verifyOnboarding(String productId, String externalId, String taxCode, String origin, String originId, String subunitCode) {
        restClient.verifyOnboarding(productId, externalId, taxCode, origin, originId, subunitCode);
    }

    @Retry(maxRetries = 3, delay = 5000, delayUnit = ChronoUnit.MILLIS, retryOn = {ProcessingException.class, IOException.class}, abortOn = {ResourceNotFoundException.class, InvalidRequestException.class, UnauthorizedUserException.class})
    public InstitutionInfo getInstitutionBillingData(String externalId, String productId) {
        BillingDataResponse response = restClient.getInstitutionBillingData(externalId, productId);
        if (response != null) {
            InstitutionInfo institutionInfo = new InstitutionInfo();
            institutionInfo.setId(response.getInstitutionId());
            institutionInfo.setExternalId(response.getExternalId());
            institutionInfo.setTaxCode(response.getTaxCode());
            institutionInfo.setDescription(response.getDescription());
            institutionInfo.setAddress(response.getAddress());
            institutionInfo.setDigitalAddress(response.getDigitalAddress());
            institutionInfo.setZipCode(response.getZipCode());
            institutionInfo.setBilling(response.getBilling());
            return institutionInfo;
        }
        return null;
    }
}
