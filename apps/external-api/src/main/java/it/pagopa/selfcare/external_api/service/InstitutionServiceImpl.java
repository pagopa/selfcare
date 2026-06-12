package it.pagopa.selfcare.external_api.service;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.CreatePgInstitutionRequest;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionResponse;
import it.pagopa.selfcare.external_api.client.*;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import it.pagopa.selfcare.external_api.mapper.ProductsMapper;
import it.pagopa.selfcare.external_api.mapper.RegistryProxyMapper;
import it.pagopa.selfcare.external_api.mapper.UserMapper;
import it.pagopa.selfcare.external_api.model.institution.GeographicTaxonomy;
import it.pagopa.selfcare.external_api.model.institution.Institution;
import it.pagopa.selfcare.external_api.model.institution.SearchMode;
import it.pagopa.selfcare.external_api.model.national_registries.LegalVerification;
import it.pagopa.selfcare.external_api.model.pnpg.CreatePnPgInstitution;
import it.pagopa.selfcare.external_api.model.product.ProductResource;
import it.pagopa.selfcare.external_api.model.user.OnboardedProductResponse;
import it.pagopa.selfcare.external_api.model.user.User;
import it.pagopa.selfcare.external_api.model.user.UserInstitution;
import it.pagopa.selfcare.external_api.model.user.UserProductResponse;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.registry_proxy.generated.openapi.v1.dto.LegalVerificationResult;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserDataResponse;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.validation.ValidationException;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.external_api.model.user.RelationshipState.ACTIVE;
import static it.pagopa.selfcare.external_api.model.user.User.Fields.*;

@Service
@Slf4j
class InstitutionServiceImpl implements InstitutionService {

    private static final EnumSet<User.Fields> USER_FIELD_LIST = EnumSet.of(name, familyName, workContacts);
    private static final EnumSet<User.Fields> USER_FIELD_LIST_FISCAL_CODE = EnumSet.of(name, familyName, workContacts, fiscalCode);
    static final String REQUIRED_INSTITUTION_MESSAGE = "An Institution id is required";
    private final MsCoreRestClient msCoreRestClient;
    private final it.pagopa.selfcare.product.service.ProductService productService;
    private static final String TAG_LOG_INSTITUTION_USER_PRODUCTS = "getInstitutionUserProducts";
    private final Set<String> serviceType;

    private final UserMapper userMapper;

    private final ProductsMapper productsMapper;


    private final MsUserApiRestClient msUserApiRestClient;
    private final MsRegistryProxyNationalRegistryRestClient nationalRegistryRestClient;
    private final RegistryProxyMapper registryProxyMapper;
    private final UserRegistryRestClient userRegistryRestClient;

    private final MsCoreInstitutionApiClient institutionApiClient;

    @Autowired
    InstitutionServiceImpl(MsCoreRestClient msCoreRestClient,
                           ProductService productService,
                           @Value("${external_api.allowed-service-types}")String[] serviceType,
                           UserMapper userMapper, ProductsMapper productsMapper, MsUserApiRestClient msUserApiRestClient,
                           MsRegistryProxyNationalRegistryRestClient nationalRegistryRestClient,
                           RegistryProxyMapper registryProxyMapper,
                           UserRegistryRestClient userRegistryRestClient, MsCoreInstitutionApiClient institutionApiClient) {
        this.msCoreRestClient = msCoreRestClient;
        this.productService = productService;
        this.serviceType = Set.of(serviceType);
        this.userMapper = userMapper;
        this.productsMapper = productsMapper;
        this.msUserApiRestClient = msUserApiRestClient;
        this.nationalRegistryRestClient = nationalRegistryRestClient;
        this.registryProxyMapper = registryProxyMapper;
        this.userRegistryRestClient = userRegistryRestClient;
        this.institutionApiClient = institutionApiClient;
    }

    @Override
    public List<GeographicTaxonomy> getGeographicTaxonomyList(String institutionId) {
        log.trace("getGeographicTaxonomyList start");
        log.debug("getGeographicTaxonomyList externalInstitutionId = {}", institutionId);
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_MESSAGE);
        Institution institution = msCoreRestClient.getInstitution(institutionId);
        if (CollectionUtils.isEmpty(institution.getGeographicTaxonomies())) {
            throw new ValidationException(String.format("The institution %s does not have geographic taxonomies.", institutionId));
        } else {
            log.debug("getGeographicTaxonomyList result = {}", institution.getGeographicTaxonomies());
            log.trace("getGeographicTaxonomyList end");
            return institution.getGeographicTaxonomies();
        }
    }


    @Override
    public Collection<Institution> getInstitutionsByGeoTaxonomies(Set<String> geoTaxIds, SearchMode searchMode) {
        log.trace("getInstitutionByGeoTaxonomy start");
        log.debug("getInstitutionByGeoTaxonomy geoTaxIds = {}, searchMode = {}", geoTaxIds, searchMode);
        Assert.notEmpty(geoTaxIds, "GeoTaxonomy ids are required in order to retrieve the institutions");
        String geoIds = String.join(",", geoTaxIds);
        Collection<Institution> institutions = msCoreRestClient.getInstitutionsByGeoTaxonomies(geoIds, searchMode).getItems();
        if (institutions == null) {
            throw new ResourceNotFoundException(String.format("No institutions where found for given taxIds = %s", geoTaxIds));
        }
        log.debug("getInstitutionByGeoTaxonomy result = {}", "null)");
        log.trace("getInstitutionByGeoTaxonomy end");
        return institutions;
    }


    @Override
    public List<ProductResource> getInstitutionUserProductsV2(String institutionId, String userId) {
        log.trace(TAG_LOG_INSTITUTION_USER_PRODUCTS + " start");
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_MESSAGE);

        List<ProductResource> productResources = new ArrayList<>();
        List<Product> products = productService.getProducts(true, true);
        if (!products.isEmpty()) {

            InstitutionResponse institutionResponse = institutionApiClient._retrieveInstitutionByIdUsingGET(institutionId, null).getBody();

            Set<String> productsSet = new HashSet<>();
            ResponseEntity<List<UserDataResponse>> response = msUserApiRestClient._retrieveUsers(institutionId, userId, userId, null, null, null, List.of(ACTIVE.name()));
            if (Objects.nonNull(response) && Objects.nonNull(response.getBody()) && !response.getBody().isEmpty()) {
                //There is only a document for the couple institutionId/userId
                productsSet = response.getBody().get(0).getProducts().stream()
                        .map(it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse::getProductId)
                        .collect(Collectors.toSet());
            }
            log.debug("getInstitutionUserProducts result = {}", products);
            log.trace("getInstitutionUserProducts start");
            List<String> productIds =  productsSet.stream().toList();
            productResources = products.stream()
                    .filter(product -> productIds.contains(product.getId()))
                    .map(product -> productsMapper.toResource(product, institutionResponse.getInstitutionType()))
                    .toList();
        }
        log.trace(TAG_LOG_INSTITUTION_USER_PRODUCTS + " result = {}", productResources);
        log.trace(TAG_LOG_INSTITUTION_USER_PRODUCTS + " end");
        return productResources;
    }

    @Override
    public Collection<UserProductResponse> getInstitutionProductUsersV2(String institutionId, String productId, String userId, Set<String> productRoles, String xSelfCareUid) {
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_MESSAGE);
        Assert.hasText(productId, "A Product id is required");
        List<String> productRolesList = CollectionUtils.isEmpty(productRoles) ? null : new ArrayList<>(productRoles);

        List<UserInstitution> usersInstitutions =  Objects.requireNonNull(msUserApiRestClient._retrievePaginatedAndFilteredUser(
                        institutionId, null, productRolesList, List.of(productId), null
                        , null, List.of(ACTIVE.name()), userId).getBody())
                .stream().map(userMapper::toUserInstitutionsFromUserInstitutionResponse).toList();

        if(CollectionUtils.isEmpty(usersInstitutions)) return List.of();
        Collection<UserProductResponse> userProductResponses = usersInstitutions.stream()
                .filter(userInstitution -> !CollectionUtils.isEmpty(userInstitution.getProducts()))
                .map(userInstitution -> {
                    UserProductResponse userProduct = new UserProductResponse();
                    userProduct.setUserMailUuid(userInstitution.getUserMailUuid());
                    userProduct.setUser(retrieveUserFromUserRegistry(userInstitution, xSelfCareUid));
                    userProduct.setRole(evaluateRoles(userInstitution));
                    userProduct.setRoles(userInstitution.getProducts().stream().map(OnboardedProductResponse::getProductRole).toList());
                    return userProduct;
                })
                .toList();

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionProductUsers result = {}", userProductResponses);
        log.trace("getInstitutionProductUsers end");
        return userProductResponses;
    }

    private PartyRole evaluateRoles(UserInstitution userInstitution) {
        List<PartyRole> roles = userInstitution.getProducts().stream()
                .map(OnboardedProductResponse::getRole)
                .filter(role -> Arrays.stream(PartyRole.values()).anyMatch(partyRole -> partyRole.name().equals(role)))
                .map(PartyRole::valueOf)
                .toList();

        return CollectionUtils.isEmpty(roles) ? null : Collections.min(roles);
    }

    private User retrieveUserFromUserRegistry(UserInstitution userInstitution, String xSelfCareUid) {
        if (StringUtils.hasText(xSelfCareUid) && serviceType.contains(xSelfCareUid)) {
            return  userRegistryRestClient.getUserByInternalId(UUID.fromString(userInstitution.getUserId()), USER_FIELD_LIST_FISCAL_CODE);
        } else {
            return userRegistryRestClient.getUserByInternalId(UUID.fromString(userInstitution.getUserId()), USER_FIELD_LIST);
        }
    }



    @Override
    public String addInstitution(CreatePnPgInstitution request) {
        log.trace("addInstitution start");
        log.debug("addInstitution request = {}", request);
        CreatePgInstitutionRequest createPgInstitutionRequest = new CreatePgInstitutionRequest(request.getDescription(), true, null, request.getExternalId());
        InstitutionResponse institutionResponse = Objects.requireNonNull(institutionApiClient.
                _createPgInstitutionUsingPOST(createPgInstitutionRequest)
                .getBody());

        log.trace("createPgInstitution end");

        String id = institutionResponse.getId();
        log.debug("addInstitution result = {}", id);
        log.trace("addInstitution end");
        return id;
    }

    @Override
    public LegalVerification verifyLegal(String taxId, String vatNumber) {
        log.trace("verifyLegal start");
        log.debug("verifyLegal taxId = {}, vatNumber = {}", Encode.forJava(taxId), Encode.forJava(vatNumber));
        ResponseEntity<LegalVerificationResult> legalVerificationResultResponseEntity = nationalRegistryRestClient._verifyLegalUsingGET(taxId, vatNumber);
        LegalVerification legalVerification = registryProxyMapper.toLegalVerification(legalVerificationResultResponseEntity.getBody());
        log.trace("verifyLegal end");
        return legalVerification;
    }

}
