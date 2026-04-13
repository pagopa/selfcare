package it.pagopa.selfcare.onboarding.service;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.client.OnboardingMsClient;
import it.pagopa.selfcare.onboarding.client.PartyClient;
import it.pagopa.selfcare.onboarding.client.PartyRegistryProxyClient;
import it.pagopa.selfcare.onboarding.client.UserRegistryClient;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.client.model.*;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.util.PgManagerVerifier;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.entity.ProductStatus;
import it.pagopa.selfcare.product.exception.ProductNotFoundException;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.onboarding_functions_json.api.OrganizationApi;
import org.openapi.quarkus.onboarding_json.model.*;
import org.owasp.encoder.Encode;
import java.util.*;
import static io.netty.util.internal.StringUtil.isNullOrEmpty;

@Slf4j
@ApplicationScoped
class InstitutionServiceImpl implements InstitutionService {
    protected static final String REQUIRED_INSTITUTION_ID_MESSAGE = "An Institution id is required";
    protected static final String REQUIRED_TAX_CODE_MESSAGE = "A taxCode id is required";
    protected static final String REQUIRED_INSTITUTION_BILLING_DATA_MESSAGE = "Institution's billing data are required";
    protected static final String REQUIRED_INSTITUTION_TYPE_MESSAGE = "An institution type is required";
    protected static final String REQUIRED_INSTITUTION_UPDATE_MESSAGE = "InsitutionUpdate is required";
    protected static final String REQUIRED_ONBOARDING_DATA_MESSAGE = "Onboarding data is required";
    protected static final String ATLEAST_ONE_PRODUCT_ROLE_REQUIRED = "At least one Product role related to %s Party role is required";
    protected static final String MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE = "More than one Product role related to %s Party role is available. Cannot automatically set the Product role";
    protected static final String A_PRODUCT_ID_IS_REQUIRED = "A Product Id is required";
    protected static final String LOCATION_INFO_IS_REQUIRED = "Location infos are required";
    private static final EnumSet<it.pagopa.selfcare.onboarding.client.model.User.Fields> USER_FIELD_LIST = EnumSet.of(User.Fields.name, User.Fields.familyName, User.Fields.workContacts);
    private static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE = "Institution with external id '%s' is not allowed to onboard '%s' product";
    public static final String UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED = "Unable to complete the onboarding for institution with taxCode '%s' to product '%s', the product is dismissed.";
    public static final String FIELD_PSP_DATA_IS_REQUIRED_FOR_PSP_INSTITUTION_ONBOARDING = "Field 'pspData' is required for PSP institution onboarding";
    public static final String ONE_OTHER_PARAMETER_PROVIDED = "At least one other parameter must be provided along with productId";
    private static final String REQUIRED_AGGREGATE_INSTITUTIONS = "Aggregate institutions are required if given institution is an Aggregator";
    private static final String ONBOARDING_COMPANY_NOT_ALLOWED = "The selected business does not belong to the user";
    private static final String PROD_PN_PG = "prod-pn-pg";
    static final String DESCRIPTION_TO_REPLACE_REGEX = " - COMUNE";
    private final OnboardingMsClient onboardingMsConnector;
    private final PartyClient partyConnector;
    private final UserRegistryClient userConnector;
    private final OrganizationApi organizationApi;
    private final PartyRegistryProxyClient partyRegistryProxyConnector;
    private final InstitutionMapper institutionMapper;
    private final OnboardingMapper onboardingMapper;
    private final PgManagerVerifier pgManagerVerifier;
    private final ProductService productService;
    InstitutionServiceImpl(OnboardingMsClient onboardingMsConnector,
                           PartyClient partyConnector,
                           ProductService productService,
                           UserRegistryClient userConnector,
                           @RestClient OrganizationApi organizationApi,
                           PartyRegistryProxyClient partyRegistryProxyConnector,
                           InstitutionMapper institutionMapper,
                           OnboardingMapper onboardingMapper,
                           PgManagerVerifier pgManagerVerifier
    ) {
        this.onboardingMsConnector = onboardingMsConnector;
        this.partyConnector = partyConnector;
        this.productService = productService;
        this.organizationApi = organizationApi;
        this.partyRegistryProxyConnector = partyRegistryProxyConnector;
        this.userConnector = userConnector;
        this.institutionMapper = institutionMapper;
        this.onboardingMapper = onboardingMapper;
        this.pgManagerVerifier = pgManagerVerifier;
    }
    @Override
    public void onboardingProductV2(OnboardingData onboardingData) {
        log.trace("onboardingProductAsync start");
        log.debug("onboardingProductAsync onboardingData = {}", onboardingData);
        onboardingMsConnector.onboarding(onboardingData);
        log.trace("onboarding end");
    }
    @Override
    public void onboardingPaAggregator(OnboardingData onboardingData) {
        log.trace("onboardingPaAggregator start");
        if(isEmptyCollection(onboardingData.getAggregates())){
            throw new ValidationException(REQUIRED_AGGREGATE_INSTITUTIONS);
        }
        onboardingMsConnector.onboardingPaAggregation(onboardingData);
        log.trace("onboarding end");
    }
    @Override
    public void onboardingCompanyV2(OnboardingData onboardingData, String userFiscalCode) {
        log.trace("onboardingProductAsync start");
        log.debug("onboardingProductAsync onboardingData = {}", onboardingData);
        verifyIfUserIsManagerOfBusiness(onboardingData.getTaxCode(), userFiscalCode, onboardingData.getOrigin());
        onboardingMsConnector.onboardingCompany(onboardingData);
        log.trace("onboarding end");
    }
    private void verifyIfUserIsManagerOfBusiness(String businessTaxCode, String userFiscalCode, String origin) {
        switch (Origin.fromValue(origin)) {
            case INFOCAMERE -> verifyIfUserIsManagerOfBusinessOnInfocamere(businessTaxCode, userFiscalCode);
            case ADE -> verifyIfUserIsManagerOfBusinessOnAde(businessTaxCode, userFiscalCode);
            default -> {
                log.error("Origin {} is not supported", origin);
                throw new InvalidRequestException("Origin not supported");
            }
        }
    }
    private void verifyIfUserIsManagerOfBusinessOnInfocamere(String businessTaxCode, String userFiscalCode) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "Checking if user with fiscal code {} is manager of business with tax code {} on Infocamere",
                userFiscalCode, businessTaxCode);
        InstitutionInfoIC userBusinesses = partyRegistryProxyConnector.getInstitutionsByUserFiscalCode(userFiscalCode);
        if (!isICBusinessRelatedToUser(userBusinesses, businessTaxCode)) {
            log.error("User is not authorized to onboard business with tax code {}", businessTaxCode);
            throw new OnboardingNotAllowedException(ONBOARDING_COMPANY_NOT_ALLOWED);
        }
    }
    private boolean isICBusinessRelatedToUser(InstitutionInfoIC institutionInfoIC, String businessTaxCode) {
        return institutionInfoIC != null
                && !isEmptyCollection(institutionInfoIC.getBusinesses())
                && institutionInfoIC.getBusinesses()
                .stream()
                .anyMatch(business -> Objects.equals(business.getBusinessTaxId(), businessTaxCode));
    }
    private void verifyIfUserIsManagerOfBusinessOnAde(String businessTaxCode, String userFiscalCode) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "Checking if user with fiscal code {} is manager of business with tax code {} on ADE",
                userFiscalCode, businessTaxCode);
        it.pagopa.selfcare.onboarding.client.model.MatchInfoResult matchInfoResult = partyRegistryProxyConnector.matchInstitutionAndUser(businessTaxCode, userFiscalCode);
        if (Objects.isNull(matchInfoResult) || !matchInfoResult.isVerificationResult()) {
            log.error("User is not authorized to onboard business with tax code {}", businessTaxCode);
            throw new OnboardingNotAllowedException(ONBOARDING_COMPANY_NOT_ALLOWED);
        }
    }
    @Override
    public void onboardingProduct(OnboardingData onboardingData) {
        log.trace("onboarding start");
        log.debug("onboarding onboardingData = {}", onboardingData);
        Objects.requireNonNull(onboardingData, REQUIRED_ONBOARDING_DATA_MESSAGE);
        Objects.requireNonNull(onboardingData.getBilling(), REQUIRED_INSTITUTION_BILLING_DATA_MESSAGE);
        Objects.requireNonNull(onboardingData.getInstitutionType(), REQUIRED_INSTITUTION_TYPE_MESSAGE);
        Objects.requireNonNull(onboardingData.getInstitutionUpdate(), REQUIRED_INSTITUTION_UPDATE_MESSAGE);
        if (InstitutionType.PSP.equals(onboardingData.getInstitutionType()) && onboardingData.getInstitutionUpdate().getPaymentServiceProvider() == null) {
            throw new ValidationException(FIELD_PSP_DATA_IS_REQUIRED_FOR_PSP_INSTITUTION_ONBOARDING);
        }
        if (isLocationInfoRequired(onboardingData.getOrigin()) && onboardingData.getLocation() == null){
            throw new ValidationException(LOCATION_INFO_IS_REQUIRED);
        }
        Product product = productService.getProduct(onboardingData.getProductId());
        Objects.requireNonNull(product, "Product is required");
        checkIfProductIsDelegable(onboardingData, product.isDelegable());
        if(product.getStatus() == ProductStatus.PHASE_OUT){
            throw new ValidationException(String.format(UNABLE_TO_COMPLETE_THE_ONBOARDING_FOR_INSTITUTION_FOR_PRODUCT_DISMISSED,
                    onboardingData.getTaxCode(),
                    product.getId()));
        }
        onboardingData.setContractPath(product.getInstitutionContractTemplate(onboardingData.getInstitutionType().name()).getContractTemplatePath());
        onboardingData.setContractVersion(product.getInstitutionContractTemplate(onboardingData.getInstitutionType().name()).getContractTemplateVersion());
        checkIfProductIsActiveAndSetUserProductRole(product, onboardingData);
        onboardingData.setProductName(product.getTitle());
        Institution institution;
        try {
            institution = partyConnector.getInstitutionsByTaxCodeAndSubunitCode(onboardingData.getTaxCode(), onboardingData.getSubunitCode())
                    .stream()
                    .findFirst()
                    .orElseThrow(ResourceNotFoundException::new);
        } catch (ResourceNotFoundException e) {
            if (InstitutionType.SA.equals(onboardingData.getInstitutionType()) && onboardingData.getOrigin().equalsIgnoreCase(Origin.ANAC.getValue())) {
                institution = partyConnector.createInstitutionFromANAC(onboardingData);
            }
            else if (InstitutionType.AS.equals(onboardingData.getInstitutionType()) && onboardingData.getOrigin().equalsIgnoreCase("IVASS")) {
                institution = partyConnector.createInstitutionFromIVASS(onboardingData);
            }
            else if (InstitutionType.PG.equals(onboardingData.getInstitutionType()) &&
                    (onboardingData.getOrigin().equalsIgnoreCase(Origin.INFOCAMERE.getValue()) || onboardingData.getOrigin().equalsIgnoreCase(Origin.ADE.getValue()))) {
                institution = partyConnector.createInstitutionFromInfocamere(onboardingData);
            }
            else if (isInstitutionPresentOnIpa(onboardingData)) {
                institution = partyConnector.createInstitutionFromIpa(
                        onboardingData.getTaxCode(),
                        onboardingData.getSubunitCode(),
                        onboardingData.getSubunitType() == null ? null : it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.valueOf(onboardingData.getSubunitType())
                );
            } else {
                institution = partyConnector.createInstitution(onboardingData);
            }
        }
        String finalInstitutionInternalId = institution.getId();
        onboardingData.getUsers().forEach(user -> {
            final Optional<it.pagopa.selfcare.onboarding.client.model.User> searchResult =
                    userConnector.search(user.getTaxCode(), USER_FIELD_LIST);
            searchResult.ifPresentOrElse(foundUser -> {
                Optional<MutableUserFieldsDto> updateRequest = createUpdateRequest(user, foundUser, finalInstitutionInternalId);
                updateRequest.ifPresent(mutableUserFieldsDto ->
                        userConnector.updateUser(UUID.fromString(foundUser.getId()), mutableUserFieldsDto));
                user.setId(foundUser.getId());
            }, () -> user.setId(userConnector.saveUser(UserMapper.toSaveUserDto(user, finalInstitutionInternalId))
                    .getId().toString()));
        });
        onboardingData.setInstitutionExternalId(institution.getExternalId());
        partyConnector.onboardingOrganization(onboardingData);
        log.trace("onboarding end");
    }
    private boolean isLocationInfoRequired(String origin) {
        return !Origin.IPA.equals(Origin.fromValue(origin)) &&
                !Origin.ADE.equals(Origin.fromValue(origin)) &&
                !Origin.INFOCAMERE.equals(Origin.fromValue(origin));
    }
    private boolean isInstitutionPresentOnIpa(OnboardingData onboardingData) {
        try {
            if (onboardingData.getSubunitType() != null && onboardingData.getSubunitType().equals("AOO")) {
                partyRegistryProxyConnector.getAooById(onboardingData.getSubunitCode());
            } else if (onboardingData.getSubunitType() != null && onboardingData.getSubunitType().equals("UO")) {
                partyRegistryProxyConnector.getUoById(onboardingData.getSubunitCode());
            } else {
                partyRegistryProxyConnector.getInstitutionProxyById(onboardingData.getTaxCode());
            }
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
    private void checkIfProductIsActiveAndSetUserProductRole(Product product, OnboardingData onboardingData) {
        Map<PartyRole, ProductRoleInfo> roleMappings;
        if (product.getParentId() != null) {
            final Product baseProduct = productService.getProduct(product.getParentId());
            if(baseProduct.getStatus() == ProductStatus.PHASE_OUT){
                throw new ValidationException(String.format("Unable to complete the onboarding for institution with taxCode '%s' to product '%s', the base product is dismissed.",
                        onboardingData.getTaxCode(),
                        baseProduct.getId()));
            }
            validateOnboardingByProductOrInstitutionTaxCode(onboardingData.getTaxCode(), baseProduct.getId());
            try {
                partyConnector.verifyOnboarding(baseProduct.getId(), null, onboardingData.getTaxCode(), onboardingData.getOrigin(), null, onboardingData.getSubunitCode());
            } catch (RuntimeException e) {
                throw new ValidationException(String.format("Unable to complete the onboarding for institution with taxCode '%s' to product '%s'. Please onboard first the '%s' product for the same institution",
                        onboardingData.getTaxCode(),
                        product.getId(),
                        baseProduct.getId()));
            }
            roleMappings = baseProduct.getRoleMappings(onboardingData.getProductId());
        } else {
            validateOnboardingByProductOrInstitutionTaxCode(onboardingData.getTaxCode(), product.getId());
            roleMappings = product.getRoleMappings(onboardingData.getProductId());
        }
        validateProductRole(onboardingData.getUsers(), roleMappings);
    }
    private void validateProductRole(List<User> users, Map<PartyRole, ProductRoleInfo> roleMappings) {
        Objects.requireNonNull(roleMappings, "Role mappings is required");
        users.forEach(userInfo -> {
            Objects.requireNonNull(roleMappings.get(userInfo.getRole()),
                    String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
            requireNotEmpty(roleMappings.get(userInfo.getRole()).getRoles(),
                    String.format(ATLEAST_ONE_PRODUCT_ROLE_REQUIRED, userInfo.getRole()));
            requireState(roleMappings.get(userInfo.getRole()).getRoles().size() == 1,
                    String.format(MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE, userInfo.getRole()));
            userInfo.setProductRole(roleMappings.get(userInfo.getRole()).getRoles().get(0).getCode());
        });
    }
    private void checkIfProductIsDelegable(OnboardingData onboardingData, boolean delegable) {
        if(InstitutionType.PT == onboardingData.getInstitutionType() && !delegable) {
            throw new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE,
                    onboardingData.getTaxCode(),
                    onboardingData.getProductId()));
        }
    }
    protected static Optional<MutableUserFieldsDto> createUpdateRequest(User user, it.pagopa.selfcare.onboarding.client.model.User foundUser, String institutionInternalId) {
        Optional<MutableUserFieldsDto> mutableUserFieldsDto = Optional.empty();
        if (isFieldToUpdate(foundUser.getName(), UserMapper.map(user.getName()))) {
            MutableUserFieldsDto dto = new MutableUserFieldsDto();
            dto.setName(user.getName());
            mutableUserFieldsDto = Optional.of(dto);
        }
        String familyName = user.getSurname() != null ? user.getSurname() : UserMapper.map(user.getFamilyName());
        if (isFieldToUpdate(foundUser.getFamilyName(), familyName)) {
            MutableUserFieldsDto dto = mutableUserFieldsDto.orElseGet(MutableUserFieldsDto::new);
            dto.setFamilyName(UserMapper.map(familyName));
            mutableUserFieldsDto = Optional.of(dto);
        }
        if (foundUser.getWorkContacts() == null
                || !foundUser.getWorkContacts().containsKey(institutionInternalId)
                || isFieldToUpdate(foundUser.getWorkContacts().get(institutionInternalId).getEmail(), UserMapper.map(user.getEmail()))) {
            MutableUserFieldsDto dto = mutableUserFieldsDto.orElseGet(MutableUserFieldsDto::new);
            final WorkContact workContact = new WorkContact();
            workContact.setEmail(user.getEmail());
            dto.setWorkContacts(Map.of(institutionInternalId, workContact));
            mutableUserFieldsDto = Optional.of(dto);
        }
        return mutableUserFieldsDto;
    }
    private static boolean isFieldToUpdate(CertifiedField<String> certifiedField, String value) {
        boolean isToUpdate = true;
        if (certifiedField != null) {
            if (Certification.NONE.equals(certifiedField.getCertification())) {
                if (certifiedField.getValue().equals(value)) {
                    isToUpdate = false;
                }
            } else {
                if (certifiedField.getValue().equalsIgnoreCase(value)) {
                    isToUpdate = false;
                } else {
                    throw new UpdateNotAllowedException(String.format("Update user request not allowed because of value %s", value));
                }
            }
        }
        return isToUpdate;
    }
    @Override
    public List<InstitutionInfo> getInstitutions(String productId, String userId) {
        log.trace("getInstitutions start");
        Product product;
        try {
            product = productService.getProduct(productId);
        } catch (ProductNotFoundException e) {
            throw new ResourceNotFoundException("No product found with id " + productId);
        }
        List<InstitutionInfo> result = partyConnector.getInstitutionsByUser(product, userId);
        log.debug("getInstitutions result = {}", result);
        log.trace("getInstitutions end");
        return result;
    }
    @Override
    public List<Institution> getActiveOnboarding(String taxCode, String productId, String subUnitCode) {
        log.trace("getActiveOnboarding start");
        log.debug("getActiveOnboarding taxCode = {}, productId = {}", Encode.forJava(taxCode), Encode.forJava(productId));
        List<Institution> institutions = partyConnector.getInstitutionsByTaxCodeAndSubunitCode(taxCode, subUnitCode);
        if (institutions.isEmpty()) {
            throw new ResourceNotFoundException("Institution not found");
        }
        List<Institution> activeOnboardingInstitutions = institutions.stream()
                .filter(institution -> !isEmptyCollection(institution.getOnboarding()))
                .peek(institution -> institution.setOnboarding(
                        institution.getOnboarding().stream()
                                .filter(onboarding -> onboarding.getProductId().equals(productId)
                                        && onboarding.getStatus().equals(String.valueOf(RelationshipState.ACTIVE)))
                                .toList()
                ))
                .filter(institution -> !institution.getOnboarding().isEmpty())
                .toList();
        if (activeOnboardingInstitutions.isEmpty()) {
            throw new ResourceNotFoundException("Institution doesn't have active onboarding for the given product");
        }
        log.debug("getActiveOnboarding result = {}", activeOnboardingInstitutions);
        log.trace("getActiveOnboarding end");
        return activeOnboardingInstitutions;
    }
    @Override
    public InstitutionOnboardingData getInstitutionOnboardingDataById(String institutionId, String productId) {
        log.trace("getInstitutionOnboardingData start");
        log.debug("getInstitutionOnboardingData institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        requireHasText(institutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        requireHasText(productId, A_PRODUCT_ID_IS_REQUIRED);
        List<OnboardingResource> onboardingsResource = partyConnector.getOnboardings(institutionId, productId);
        OnboardingResource onboardingResource = onboardingsResource.stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Onboarding for institutionId %s not found", institutionId)));
        Institution institution = partyConnector.getInstitutionById(institutionId);
        InstitutionOnboardingData result = new InstitutionOnboardingData();
        InstitutionInfo institutionInfo = institutionMapper.toInstitutionInfo(institution);
        institutionInfo.setPricingPlan(onboardingResource.getPricingPlan());
        institutionInfo.setBilling(onboardingResource.getBilling());
        result.setInstitution(institutionInfo);
        result.setGeographicTaxonomies(institution.getGeographicTaxonomies());
        result.setCompanyInformations(institution.getCompanyInformations());
        result.setAssistanceContacts(institution.getAssistanceContacts());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionOnboardingData result = {}", result);
        log.trace("getInstitutionOnboardingData end");
        return result;
    }
    @Override
    public Institution getInstitutionByExternalId(String externalInstitutionId) {
        log.trace("getInstitutionData start");
        log.debug("getInstitutionData externalInstitutionId = {}", externalInstitutionId);
        requireHasText(externalInstitutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        Institution institution = partyConnector.getInstitutionByExternalId(externalInstitutionId);
        log.debug("getInstitutionData result = {}", institution);
        log.trace("getInstitutionData end");
        return institution;
    }
    @Override
    public List<it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy> getGeographicTaxonomyList(String externalInstitutionId) {
        log.trace("geographicTaxonomyList start");
        log.debug("geographicTaxonomyList externalInstitutionId = {}", externalInstitutionId);
        requireHasText(externalInstitutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        Institution institution = partyConnector.getInstitutionByExternalId(externalInstitutionId);
        List<it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy> result = Optional.ofNullable(institution.getGeographicTaxonomies())
                .orElse(Collections.emptyList());
        log.debug("geographicTaxonomyList result = {}", result);
        log.trace("geographicTaxonomyList end");
        return result;
    }
    @Override
    public List<it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy> getGeographicTaxonomyList(String taxCode, String subunitCode) {
        requireHasText(taxCode, REQUIRED_TAX_CODE_MESSAGE);
        List<Institution> institutions = partyConnector.getInstitutionsByTaxCodeAndSubunitCode(taxCode, subunitCode);
        if(Objects.isNull(institutions) || institutions.isEmpty()) return Collections.emptyList();
        return Optional.ofNullable(institutions.get(0).getGeographicTaxonomies())
                .orElse(Collections.emptyList());
    }
    @Override
    public void verifyOnboarding(String externalInstitutionId, String productId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding externalInstitutionId = {}", externalInstitutionId);
        validateOnboardingByProductOrInstitutionTaxCode(externalInstitutionId, productId);
        partyConnector.verifyOnboarding(externalInstitutionId, productId);
        log.trace("verifyOnboarding end");
    }
    @Override
    public void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType) {
        log.trace("verifyOnboardingSubunit start");
        validateParameter(taxCode, origin, originId, subunitCode);
        log.debug("verifyOnboardingSubunit taxCode = {}", taxCode);
        validateOnboardingByProductOrInstitutionTaxCode(taxCode, productId);
        onboardingMsConnector.verifyOnboarding(productId, taxCode, origin, originId, subunitCode, institutionType);
        log.trace("verifyOnboardingSubunit end");
    }
    private void validateParameter(String taxCode, String origin, String originId, String subunitCode) {
        if (isNullOrEmpty(taxCode) && isNullOrEmpty(origin) && isNullOrEmpty(originId) && isNullOrEmpty(subunitCode)) {
            log.error("other parameters are missing while only productId is provided");
            throw new InvalidRequestException(String.format(ONE_OTHER_PARAMETER_PROVIDED));
        }
    }
    @Override
    public void checkOrganization(String productId, String fiscalCode, String vatNumber) {
        log.trace("checkOrganization start");
        log.debug("checkOrganization productId = {}, fiscalCode = {}, vatNumber = {}", productId, fiscalCode, vatNumber );
        organizationApi.checkOrganization(fiscalCode, vatNumber).await().indefinitely();
        log.trace("checkOrganization end");
    }
    public void validateOnboardingByProductOrInstitutionTaxCode(String externalInstitutionId, String productId) {
        log.trace("validate start");
        log.debug("validate productId = {}, externalInstitutionId = {}", productId, externalInstitutionId);
        boolean productEnabled = productService.isProductEnabled(productId);
        boolean institutionAllowed = productService.verifyAllowedByInstitutionTaxCode(productId, externalInstitutionId);
        log.debug("validate result = {}", productEnabled || institutionAllowed);
        log.trace("validate end");
        if (!productEnabled && !institutionAllowed) {
            throw new OnboardingNotAllowedException(String.format(ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_TEMPLATE,
                    externalInstitutionId,
                    productId));
        }
    }
    @Override
    public InstitutionInfoIC getInstitutionsByUser(String fiscalCode) {
        log.trace("getInstitutionsByUserId start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsByUserId user = {}", fiscalCode);
        InstitutionInfoIC result = partyRegistryProxyConnector.getInstitutionsByUserFiscalCode(fiscalCode);
        log.debug("found {} institutions for the user", result.getBusinesses().size());
        List<BusinessInfoIC> institutionsNotOnboardedByUser = result.getBusinesses().stream()
                .filter(businessInfoIC -> !isOnboardedByUser(businessInfoIC, fiscalCode))
                .toList();
        result.setBusinesses(institutionsNotOnboardedByUser);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionsByUserId result = {}", result);
        log.trace("getInstitutionsByUserId end");
        return result;
    }
    private boolean isOnboardedByUser(BusinessInfoIC businessInfoIC, String fiscalCode) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "Checking if business with tax code {} is onboarded by user with fiscal code {}",
                businessInfoIC.getBusinessTaxId(), fiscalCode);
        try {
            onboardingMsConnector.verifyOnboarding(PROD_PN_PG, businessInfoIC.getBusinessTaxId(), null, null, null, null);
            log.debug("Business with tax code {} is already onboarded, checking if user with fiscal code {} is manager",
                    businessInfoIC.getBusinessTaxId(), fiscalCode);
            CheckManagerRequest request = onboardingMapper.toCheckManagerRequest(null, businessInfoIC.getBusinessTaxId(), PROD_PN_PG);
            boolean isManager = onboardingMsConnector.checkManager(request).getResponse();
            log.debug(LogUtils.CONFIDENTIAL_MARKER, "User with fiscal code {} is manager of business with tax code {}",
                    fiscalCode, businessInfoIC.getBusinessTaxId());
            return isManager;
        } catch (ResourceNotFoundException e) {
            log.debug("Business with tax code {} is not onboarded", businessInfoIC.getBusinessTaxId());
            return false;
        }
    }
    @Override
    public List<Institution> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode) {
        log.trace("getByFilters start");
        List<org.openapi.quarkus.onboarding_json.model.OnboardingResponse> result = onboardingMsConnector.getByFilters(productId, taxCode, origin, originId, subunitCode);
        if(Objects.isNull(result) || result.isEmpty()) {
            throw new ResourceNotFoundException();
        }
        log.trace("getByFilters end");
        List<Institution> institutions = result.stream()
                .map(org.openapi.quarkus.onboarding_json.model.OnboardingResponse::getInstitution)
                .map(institutionMapper::toInstitution).toList();
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getByFilters result = {}", institutions);
        return institutions;
    }
    @Override
    public MatchInfoResult matchInstitutionAndUser(String externalInstitutionId, User user) {
        log.trace("matchInstitutionAndUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser user = {}", user);
        MatchInfoResult result = partyRegistryProxyConnector.matchInstitutionAndUser(externalInstitutionId, user.getTaxCode());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "matchInstitutionAndUser result = {}", result);
        log.trace("matchInstitutionAndUser end");
        return result;
    }
    @Override
    public InstitutionLegalAddressData getInstitutionLegalAddress(String externalInstitutionId) {
        log.trace("getInstitutionLegalAddress start");
        log.debug("getInstitutionLegalAddress externalInstitutionId = {}", externalInstitutionId);
        InstitutionLegalAddressData result = partyRegistryProxyConnector.getInstitutionLegalAddress(externalInstitutionId);
        log.debug("getInstitutionLegalAddress result = {}", result);
        log.trace("getInstitutionLegalAddress end");
        return result;
    }
    @Override
    public org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse validateAggregatesCsv(UploadedFile file, String productId) {
        log.trace("validateAggregatesCsv start");
        log.debug("validateAggregatesCsv productId = {}", productId);
        return onboardingMsConnector.aggregatesVerification(file, productId);
    }
    @Override
    public org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus  checkRecipientCode(String originId, String recipientCode) {
        log.trace("checkRecipientCode start");
        log.debug("checkRecipientCode for institution with originId {} and recipientCode {}", originId, recipientCode);
        return onboardingMsConnector.checkRecipientCode(originId, recipientCode);
    }
    @Override
    public void onboardingUsersPgFromIcAndAde(OnboardingData onboardingData) {
        log.trace("onboardingUsersPgFromIcAndAde start");
        log.debug("onboardingUsersPgFromIcAndAde onboardingData = {}", Encode.forJava(onboardingData.toString()));
        onboardingMsConnector.onboardingUsersPgFromIcAndAde(onboardingData);
        log.trace("onboardingUsersPgFromIcAndAde end");
    }
    @Override
    public ManagerVerification verifyManager(String userTaxCode, String institutionTaxCode) {
        log.trace("verifyManager start");
        ManagerVerification result = pgManagerVerifier.doVerify(userTaxCode, institutionTaxCode);
        if(!result.isVerified()) {
            throw new ResourceNotFoundException(String.format("User with userTaxCode %s is not the legal representative of the institution", userTaxCode));
        }
        return result;
    }
    @Override
    public InstitutionOnboardingData getInstitutionOnboardingData(String externalInstitutionId, String productId) {
        log.trace("getInstitutionOnboardingData start");
        log.debug("getInstitutionOnboardingData externalInstitutionId = {}, productId = {}", externalInstitutionId, productId);
        requireHasText(externalInstitutionId, REQUIRED_INSTITUTION_ID_MESSAGE);
        requireHasText(productId, A_PRODUCT_ID_IS_REQUIRED);
        InstitutionOnboardingData result = new InstitutionOnboardingData();
        InstitutionInfo institutionInfo = partyConnector.getInstitutionBillingData(externalInstitutionId, productId);
        if (institutionInfo == null) {
            throw new ResourceNotFoundException(String.format("Institution %s not found", externalInstitutionId));
        }
        Institution institution = partyConnector.getInstitutionByExternalId(externalInstitutionId);
        if (institution == null) {
            throw new ResourceNotFoundException(String.format("Institution %s not found", externalInstitutionId));
        }
        if (institution.getGeographicTaxonomies() == null) {
            throw new ValidationException(String.format("The institution %s does not have geographic taxonomies.", externalInstitutionId));
        }
        setInstitutionInfo(institution, institutionInfo);
        setLocationInfo(institutionInfo);
        result.setInstitution(institutionInfo);
        result.setGeographicTaxonomies(institution.getGeographicTaxonomies());
        result.setCompanyInformations(institution.getCompanyInformations());
        result.setAssistanceContacts(institution.getAssistanceContacts());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionOnboardingData result = {}", result);
        log.trace("getInstitutionOnboardingData end");
        return result;
    }
    private void setInstitutionInfo(Institution institution, InstitutionInfo institutionInfo){
        InstitutionLocation institutionLocation = new InstitutionLocation();
        institutionLocation.setCountry(institution.getCountry());
        institutionLocation.setCity(institution.getCity());
        institutionLocation.setCounty(institution.getCounty());
        institutionInfo.setInstitutionLocation(institutionLocation);
        institutionInfo.setSubunitCode(institution.getSubunitCode());
        institutionInfo.setSubunitType(institution.getSubunitType());
        institutionInfo.setOrigin(institution.getOrigin());
    }
    private void setLocationInfo(InstitutionInfo institutionInfo){
        if (institutionInfo.getInstitutionLocation().getCity()==null && Origin.IPA.getValue().equals(institutionInfo.getOrigin())){
            try {
                GeographicTaxonomiesResponse geographicTaxonomies;
                if (institutionInfo.getSubunitType() != null) {
                    geographicTaxonomies = switch (Objects.requireNonNull(institutionInfo.getSubunitType())) {
                        case "UO" -> {
                            UoResponse organizationUnit = partyRegistryProxyConnector.getUoById(institutionInfo.getSubunitCode());
                            yield partyRegistryProxyConnector.getExtById(organizationUnit.getMunicipalIstatCode());
                        }
                        case "AOO" -> {
                            AooResponse homogeneousOrganizationalArea = partyRegistryProxyConnector.getAooById(institutionInfo.getSubunitCode());
                            yield partyRegistryProxyConnector.getExtById(homogeneousOrganizationalArea.getMunicipalIstatCode());
                        }
                        default -> {
                            ProxyInstitutionResponse proxyInfo = partyRegistryProxyConnector.getInstitutionProxyById(institutionInfo.getTaxCode());
                            yield partyRegistryProxyConnector.getExtById(proxyInfo.getIstatCode());
                        }
                    };
                }
                else {
                    ProxyInstitutionResponse proxyInfo = partyRegistryProxyConnector.getInstitutionProxyById(institutionInfo.getTaxCode());
                    geographicTaxonomies = partyRegistryProxyConnector.getExtById(proxyInfo.getIstatCode());
                }
                if (geographicTaxonomies != null) {
                    institutionInfo.getInstitutionLocation().setCounty(geographicTaxonomies.getProvinceAbbreviation());
                    institutionInfo.getInstitutionLocation().setCountry(geographicTaxonomies.getCountryAbbreviation());
                    institutionInfo.getInstitutionLocation().setCity(geographicTaxonomies.getDescription().replace(DESCRIPTION_TO_REPLACE_REGEX, ""));
                }
            } catch (ResourceNotFoundException e) {
                log.warn("Error while searching institution {} on IPA, {} ", institutionInfo.getDescription(), e.getMessage());
            }
        }
    }
    public OnboardingGetResponse getOnboardingWithFilter(String inputTaxCode, String inputStatus) {
        log.trace("getOnboardingWithFilter start");
        String taxCode = Encode.forJava(inputTaxCode);
        String status = Encode.forJava(inputStatus);
        log.debug("getOnboardingWithFilter with taxCode = {}, stauts = {}", taxCode, status);
        OnboardingGetResponse result =  onboardingMsConnector.onboardingWithFilter(taxCode, status);
        log.trace("getOnboardingWithFilter end");
        return result;
    }

    private static boolean isEmptyCollection(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private static void requireHasText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNotEmpty(Collection<?> values, String message) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
