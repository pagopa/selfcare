package it.pagopa.selfcare.onboarding.connector;

import static it.pagopa.selfcare.onboarding.connector.model.RelationshipState.ACTIVE;

import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.onboarding.connector.api.PartyConnector;
import it.pagopa.selfcare.onboarding.connector.model.RelationshipInfo;
import it.pagopa.selfcare.onboarding.connector.model.RelationshipsResponse;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.institutions.InstitutionInfo;
import it.pagopa.selfcare.onboarding.connector.model.institutions.OnboardingResource;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.UserInfo;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsOnboardingInstitutionApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsUserApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.client.PartyProcessRestClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.connector.rest.model.*;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.GetInstitutionRequest;
import it.pagopa.selfcare.product.entity.Product;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Slf4j
class PartyConnectorImpl implements PartyConnector {

    protected static final String REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE = "An Institution external id is required";
    protected static final String REQUIRED_INSTITUTION_ID_MESSAGE = "An Institution id is required";
    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product Id is required";
    protected static final String REQUIRED_INSTITUTION_TAXCODE_MESSAGE = "An Institution tax code is required";
    private final PartyProcessRestClient restClient;
    private final InstitutionMapper institutionMapper;
    private final MsUserApiClient userApiClient;
    private final MsOnboardingInstitutionApiClient institutionApiClient;

    static final Function<RelationshipInfo, UserInfo> RELATIONSHIP_INFO_TO_USER_INFO_FUNCTION = relationshipInfo -> {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(relationshipInfo.getFrom());
        userInfo.setStatus(relationshipInfo.getState().toString());
        userInfo.setRole(relationshipInfo.getRole());
        userInfo.setInstitutionId(relationshipInfo.getTo());
        return userInfo;
    };

    private Map<String, it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.InstitutionResponse> buildInstitutionMap(List<InstitutionInfo> result) {
        GetInstitutionRequest request = new GetInstitutionRequest();
        request.setInstitutionIds(result.stream().map(InstitutionInfo::getId).toList());
        List<it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.InstitutionResponse> response = institutionApiClient._getInstitutions(request).getBody();
        return Objects.isNull(response) ? Map.of() : response.stream().collect(Collectors.toMap(it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.InstitutionResponse::getId, Function.identity()));
    }

    @Autowired
    public PartyConnectorImpl(PartyProcessRestClient restClient,
                              InstitutionMapper institutionMapper,
                              MsUserApiClient userApiClient,
                              MsOnboardingInstitutionApiClient institutionApiClient) {
        this.restClient = restClient;
        this.institutionMapper = institutionMapper;
        this.userApiClient = userApiClient;
        this.institutionApiClient = institutionApiClient;
    }

    @Override
    public void onboardingOrganization(OnboardingData onboardingData) {
        Assert.notNull(onboardingData, "Onboarding data is required");
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
        institutionUpdate.setAdditionalInformations(onboardingData.getInstitutionUpdate().getAdditionalInformations());
        onboardingInstitutionRequest.setInstitutionUpdate(institutionUpdate);

        onboardingInstitutionRequest.setUsers(onboardingData.getUsers().stream()
                .map(userInfo -> {
                    User user = new User();
                    user.setId(userInfo.getId());
                    user.setName(userInfo.getName());
                    user.setSurname(userInfo.getSurname());
                    user.setTaxCode(userInfo.getTaxCode());
                    user.setEmail(userInfo.getEmail());
                    user.setRole(userInfo.getRole());
                    user.setProductRole(userInfo.getProductRole());
                    return user;
                }).toList());
        OnboardingContract onboardingContract = new OnboardingContract();
        onboardingContract.setPath(onboardingData.getContractPath());
        onboardingContract.setVersion(onboardingData.getContractVersion());
        onboardingInstitutionRequest.setContract(onboardingContract);

        restClient.onboardingOrganization(onboardingInstitutionRequest);
    }

    @Override
    public List<InstitutionInfo> getInstitutionsByUser(Product product, String userId) {
        log.trace("getInstitutionsByUser start");
        final String parentProductId = product.getParentId();
        List<UserInstitutionResponse> userInstitutions = userApiClient._usersGet(null, null, null, Optional.ofNullable(product.getId()).map(List::of).orElse(null), null, 500, List.of(ACTIVE.name()), userId).getBody();
        List<InstitutionInfo> result;

        if (Objects.nonNull(parentProductId)) {

            List<UserInstitutionResponse> parentUserInstitutions = userApiClient._usersGet(null, null, null, Optional.ofNullable(parentProductId).map(List::of).orElse(null), null, 500, List.of(ACTIVE.name()), userId).getBody();

            // Get institution identifiers from list linked to the product
            List<String> childInstitutionIds = userInstitutions.stream()
                    .map(UserInstitutionResponse::getInstitutionId)
                    .toList();

            // Filtering objects from the first list not included into second one (linked to parent product)
            result  = parentUserInstitutions.stream()
                    .filter(parentInstitution -> !childInstitutionIds.contains(parentInstitution.getInstitutionId()))
                    .map(institutionMapper::toInstitutionInfo)
                    .toList();

        } else {
            result = Objects.requireNonNull(userInstitutions).stream()
                    .map(institutionMapper::toInstitutionInfo)
                    .toList();
        }

        Map<String, it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.InstitutionResponse> map = buildInstitutionMap(result);

        // Filtering result for allowed institution types on product
        List<InstitutionInfo> allowedInstitutions = Objects.isNull(product.getInstitutionTypesAllowed())
                || product.getInstitutionTypesAllowed().isEmpty()
                ? result
                : result.stream()
                .filter(institutionInfo -> map.containsKey(institutionInfo.getId()) &&
                        product.getInstitutionTypesAllowed().contains(map.get(institutionInfo.getId()).getInstitutionType()))
                .toList();

        log.debug("getInstitutionsByUser result = {}", allowedInstitutions);
        log.trace("getInstitutionsByUser end");
        return allowedInstitutions;
    }

    @Override
    public RelationshipsResponse getUserInstitutionRelationships(String externalInstitutionId, UserInfo.UserInfoFilter userInfoFilter) {
        log.trace("getUserInstitutionRelationships start");
        log.debug("getUserInstitutionRelationships externalInstitutionId = {}, userInfoFilter = {}", externalInstitutionId, userInfoFilter);
        Assert.hasText(externalInstitutionId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);
        Assert.notNull(userInfoFilter, "A filter is required");
        RelationshipsResponse institutionRelationships = restClient.getUserInstitutionRelationships(
                externalInstitutionId,
                userInfoFilter.getRole().orElse(null),
                userInfoFilter.getAllowedStates().orElse(null),
                userInfoFilter.getProductId().map(Set::of).orElse(null),
                userInfoFilter.getProductRoles().orElse(null),
                userInfoFilter.getUserId().orElse(null)
        );
        log.debug("getUserInstitutionRelationships institutionRelationships = {}", institutionRelationships);
        log.trace("getUserInstitutionRelationships end");
        return institutionRelationships;
    }

    @Override
    public Collection<UserInfo> getUsers(String externalInstitutionId, UserInfo.UserInfoFilter userInfoFilter) {
        log.trace("getUsers start");
        log.debug("getUsers externalInstitutionId = {}, role = {}, productId = {}, productRoles = {}, userId = {}", externalInstitutionId, userInfoFilter.getRole(), userInfoFilter.getProductId(), userInfoFilter.getProductRoles(), userInfoFilter.getUserId());
        Assert.hasText(externalInstitutionId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);

        Collection<UserInfo> userInfos = Collections.emptyList();
        RelationshipsResponse institutionRelationships = restClient.getUserInstitutionRelationships(externalInstitutionId,
                userInfoFilter.getRole().orElse(null),
                userInfoFilter.getAllowedStates().orElse(null),
                userInfoFilter.getProductId().map(Set::of).orElse(null),
                userInfoFilter.getProductRoles().orElse(null),
                userInfoFilter.getUserId().orElse(null));
        if (institutionRelationships != null) {
            userInfos = institutionRelationships.stream()
                    .collect(Collectors.toMap(RelationshipInfo::getFrom,
                            RELATIONSHIP_INFO_TO_USER_INFO_FUNCTION, (userInfo1, userInfo2) -> {
                                if (userInfo1.getStatus().equals(userInfo2.getStatus())) {
                                    if (userInfo1.getRole().compareTo(userInfo2.getRole()) > 0) {
                                        userInfo1.setRole(userInfo2.getRole());
                                    }
                                } else {
                                    if ("ACTIVE".equals(userInfo2.getStatus())) {
                                        userInfo1.setRole(userInfo2.getRole());
                                        userInfo1.setStatus(userInfo2.getStatus());
                                    }
                                }
                                return userInfo1;
                            })).values();
        }
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUsers result = {}", userInfos);
        log.trace("getUsers end");
        return userInfos;
    }

    @Override
    @Retry(name = "retryTimeout")
    public List<Institution> getInstitutionsByTaxCodeAndSubunitCode(String taxCode, String subunitCode) {
        log.trace("getInstitution start");
        log.debug("getInstitution taxCode = {}, subunitCode = {}", Encode.forJava(taxCode), Encode.forJava(subunitCode));
        Assert.hasText(taxCode, REQUIRED_INSTITUTION_TAXCODE_MESSAGE);
        InstitutionsResponse partyInstitutionResponse = restClient.getInstitutions(taxCode, subunitCode);
        List<Institution> result = partyInstitutionResponse.getInstitutions().stream()
                .map(institutionMapper::toEntity)
                .toList();
        log.debug("getInstitution result = {}", result);
        log.trace("getInstitution end");
        return result;
    }

    @Override
    public Institution getInstitutionByExternalId(String externalInstitutionId) {
        log.trace("getInstitution start");
        log.debug("getInstitution externalInstitutionId = {}", externalInstitutionId);
        Assert.hasText(externalInstitutionId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);
        InstitutionResponse partyInstitutionResponse = restClient.getInstitutionByExternalId(externalInstitutionId);
        Institution result = institutionMapper.toEntity(partyInstitutionResponse);
        log.debug("getInstitution result = {}", result);
        log.trace("getInstitution end");
        return result;
    }

    @Override
    public Institution getInstitutionById(String institutionId) {
        log.trace("getInstitutionById start");
        log.debug("getInstitutionById institutionId = {}", Encode.forJava(institutionId));
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        InstitutionResponse institutionResponse = restClient.getInstitutionById(institutionId);
        Institution result = institutionMapper.toEntity(institutionResponse);
        log.debug("getInstitutionById result = {}", result);
        log.trace("getInstitutionById end");
        return result;
    }

    @Override
    public List<OnboardingResource> getOnboardings(String institutionId, String productId) {
        log.trace("getOnboardings start");
        log.debug("getOnboardings institutionId = {}", Encode.forJava(institutionId));
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        OnboardingsResponse onboardings = restClient.getOnboardings(institutionId, productId);
        List<OnboardingResource> onboardingResources = onboardings.getOnboardings().stream()
                .map(institutionMapper::toResource)
                .toList();
        log.debug("getOnboardings result = {}", onboardingResources);
        log.trace("getOnboardings end");
        return onboardingResources;
    }

    @Override
    public Institution createInstitutionFromIpa(String taxCode, String subunitCode, String subunitType) {
        log.trace("createInstitutionFromIpa start");
        log.debug("createInstitutionFromIpa taxCode = {}, subunitCode = {}, subunitType = {}", taxCode, subunitCode, subunitType);
        Assert.hasText(taxCode, REQUIRED_INSTITUTION_TAXCODE_MESSAGE);
        InstitutionFromIpaPost institutionFromIpaPost = new InstitutionFromIpaPost();
        institutionFromIpaPost.setSubunitCode(subunitCode);
        institutionFromIpaPost.setTaxCode(taxCode);
        institutionFromIpaPost.setSubunitType(subunitType);
        InstitutionResponse partyInstitutionResponse = restClient.createInstitutionFromIpa(institutionFromIpaPost);
        Institution result = institutionMapper.toEntity(partyInstitutionResponse);
        log.debug("createInstitutionFromIpa result = {}", result);
        log.trace("createInstitutionFromIpa end");
        return result;
    }

    @Override
    public Institution createInstitutionFromANAC(OnboardingData onboardingData) {
        log.trace("createInstitutionFromAnac start");
        Assert.notNull(onboardingData, "An OnboardingData is required");
        InstitutionResponse partyInstitutionResponse = restClient.createInstitutionFromANAC(new InstitutionSeed(onboardingData));
        Institution result = institutionMapper.toEntity(partyInstitutionResponse);
        log.debug("createInstitutionFromAnac result = {}", result);
        log.trace("createInstitutionFromAnac end");
        return result;
    }

    @Override
    public Institution createInstitutionFromIVASS(OnboardingData onboardingData) {
        log.trace("createInstitutionFromIVASS start");
        Assert.notNull(onboardingData, "An OnboardingData is required");
        InstitutionResponse partyInstitutionResponse = restClient.createInstitutionFromIVASS(new InstitutionSeed(onboardingData));
        Institution result = institutionMapper.toEntity(partyInstitutionResponse);
        log.debug("createInstitutionFromIVASS result = {}", result);
        log.trace("createInstitutionFromIVASS end");
        return result;
    }

    @Override
    public Institution createInstitutionUsingExternalId(String institutionExternalId) {
        log.trace("createInstitutionUsingExternalId start");
        log.debug("createInstitutionUsingExternalId externalId = {}", institutionExternalId);
        Assert.hasText(institutionExternalId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);
        InstitutionResponse partyInstitutionResponse = restClient.createInstitutionUsingExternalId(institutionExternalId);
        Institution result = institutionMapper.toEntity(partyInstitutionResponse);
        log.debug("createInstitutionUsingExternalId result = {}", result);
        log.trace("createInstitutionUsingExternalId end");
        return result;
    }

    @Override
    public Institution createInstitutionFromInfocamere(OnboardingData onboardingData) {
        log.trace("createInstitutionFromInfocamere start");
        Assert.notNull(onboardingData, "An OnboardingData is required");
        InstitutionResponse partyInstitutionResponse = restClient.createInstitutionFromInfocamere(new InstitutionSeed(onboardingData));
        Institution result = institutionMapper.toEntity(partyInstitutionResponse);
        log.debug("createInstitutionFromInfocamere result = {}", result);
        log.trace("createInstitutionFromInfocamere end");
        return result;
    }

    @Override
    public Institution createInstitution(OnboardingData onboardingData) {
        log.trace("createInstitution start");
        Assert.notNull(onboardingData, "An OnboardingData is required");
        InstitutionResponse partyInstitutionResponse = restClient.createInstitution(new InstitutionSeed(onboardingData));
        Institution result = institutionMapper.toEntity(partyInstitutionResponse);
        log.debug("createInstitution result = {}", result);
        log.trace("createInstitution end");
        return result;
    }

    @Override
    public UserInfo getInstitutionManager(String externalInstitutionId, String productId) {
        log.trace("getInstitutionManager start");
        log.debug("getInstitutionManager externalId = {}, productId = {}", externalInstitutionId, productId);
        Assert.hasText(externalInstitutionId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);
        Assert.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        RelationshipInfo relationshipInfo = restClient.getInstitutionManager(externalInstitutionId, productId);
        UserInfo result = RELATIONSHIP_INFO_TO_USER_INFO_FUNCTION.apply(relationshipInfo);
        log.debug("getInstitutionManager result = {}", result);
        log.trace("getInstitutionManager end");
        return result;
    }

    @Override
    public InstitutionInfo getInstitutionBillingData(String externalId, String productId) {
        log.trace("getInstitutionBillingData start");
        log.debug("getInstitutionBillingData externalId = {}, productId = {}", externalId, productId);
        Assert.hasText(externalId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);
        Assert.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        BillingDataResponse billingDataResponse = restClient.getInstitutionBillingData(externalId, productId);
        InstitutionInfo result = institutionMapper.toInstitutionInfo(billingDataResponse);
        log.debug("getInstitutionBillingData result = {}", result);
        log.trace("getInstitutionBillingData end");
        return result;
    }

    @Override
    public void verifyOnboarding(String externalInstitutionId, String productId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding externalInstitutionId = {}, productId = {}", externalInstitutionId, productId);
        Assert.hasText(externalInstitutionId, REQUIRED_INSTITUTION_EXTERNAL_ID_MESSAGE);
        Assert.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        restClient.verifyOnboarding(externalInstitutionId, productId);
        log.trace("verifyOnboarding end");
    }


    @Override
    public void verifyOnboarding(String productId, String externalId, String taxCode, String origin, String originId, String subunitCode) {
        log.trace("verifyOnboarding start");
        Assert.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        restClient._verifyOnboardingInfoByFiltersUsingHEAD(productId, externalId, taxCode, origin, originId, subunitCode);
        log.trace("verifyOnboarding end");
    }

}
