package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.dashboard.client.CoreInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.client.UserApiRestClient;
import it.pagopa.selfcare.dashboard.client.UserInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.exception.InvalidOnboardingStatusException;
import it.pagopa.selfcare.dashboard.exception.InvalidProductRoleException;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.institution.InstitutionBase;
import it.pagopa.selfcare.dashboard.model.institution.OnboardedProduct;
import it.pagopa.selfcare.dashboard.model.institution.RelationshipState;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionMapper;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapper;
import it.pagopa.selfcare.dashboard.model.product.mapper.ProductMapper;
import it.pagopa.selfcare.dashboard.model.user.*;
import it.pagopa.selfcare.dashboard.model.user.CreateUserDto;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.dashboard.model.institution.RelationshipState.*;


@Slf4j
@Service
public class UserV2ServiceImpl implements UserV2Service {

    private final UserGroupV2Service userGroupService;
    private final ProductService productService;
    private final UserApiRestClient userApiRestClient;
    private final CoreInstitutionApiRestClient coreInstitutionApiRestClient;
    private final InstitutionMapper institutionMapper;
    private final UserMapper userMapper;
    private final UserInstitutionApiRestClient userInstitutionApiRestClient;

    private final List<RelationshipState> allowedStates;
    static final String REQUIRED_INSTITUTION_ID_MESSAGE = "An Institution id is required";

    public UserV2ServiceImpl(
            UserGroupV2Service userGroupService, ProductService productService,
            UserApiRestClient userApiRestClient, CoreInstitutionApiRestClient coreInstitutionApiRestClient,
            InstitutionMapper institutionMapper, UserMapper userMapper, UserInstitutionApiRestClient userInstitutionApiRestClient,
            @Value("${dashboard.institution.getUsers.filter.states}") String[] allowedStates
    ) {
        this.userGroupService = userGroupService;
        this.productService = productService;
        this.userApiRestClient = userApiRestClient;
        this.coreInstitutionApiRestClient = coreInstitutionApiRestClient;
        this.institutionMapper = institutionMapper;
        this.userMapper = userMapper;
        this.userInstitutionApiRestClient = userInstitutionApiRestClient;
        this.allowedStates = allowedStates != null && allowedStates.length != 0 ? Arrays.stream(allowedStates).map(RelationshipState::valueOf).toList() : null;
    }


    @Override
    public Collection<InstitutionBase> getInstitutions(String userId) {
        log.trace("getInstitutions start");
        Collection<InstitutionBase> result = getUserInstitutions(userId);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutions result = {}", result);
        log.trace("getInstitutions end");
        return result;
    }

    private List<InstitutionBase> getUserInstitutions(String userId) {
        log.trace("getUserProducts start");
        UserInfoResponse userInfoResponse;
        try {
            userInfoResponse = userApiRestClient._getUserProductsInfo(userId, null,
                    List.of(ACTIVE.name(), PENDING.name(), TOBEVALIDATED.name())).getBody();
        } catch (ResourceNotFoundException ex) {
            log.debug("getUserProducts - User with id {} not found", userId);
            return List.of();
        }

        if (Objects.isNull(userInfoResponse) ||
                Objects.isNull(userInfoResponse.getInstitutions())) return List.of();

        List<InstitutionBase> result = userInfoResponse.getInstitutions().stream()
                .map(institutionMapper::toInstitutionBase)
                .sorted(Comparator.comparing(InstitutionBase::getName, Comparator.nullsLast(String::compareTo)))
                .toList();
        log.debug("getUserProducts result = {}", result);
        log.trace("getUserProducts end");
        return result;
    }

    @Override
    public void deleteUserProduct(String userId, String institutionId, String productId, String productRole) {
        log.trace("delete start");
        log.debug("delete userId = {} for institutionId = {}, productId = {} and productRole = {}", Encode.forJava(userId) , Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(productRole));
        userApiRestClient._updateUserProductStatus(userId, institutionId, productId, OnboardedProductState.DELETED, productRole);
        userGroupService.deleteMembersByUserId(userId, institutionId, productId);
        log.trace("delete end");
    }

    @Override
    public void activateUserProduct(String userId, String institutionId, String productId, String productRole) {
        log.trace("activate start");
        log.debug("activate userId = {} for institutionId = {}, productId = {} and productRole = {}", Encode.forJava(userId), Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(productRole));
        userApiRestClient._updateUserProductStatus(userId, institutionId, productId, OnboardedProductState.ACTIVE, productRole);
        log.trace("activate end");

    }

    @Override
    public void suspendUserProduct(String userId, String institutionId, String productId, String productRole) {
        log.trace("suspend start");
        log.debug("suspend userId = {} for institutionId = {} and product = {}", Encode.forJava(userId), Encode.forJava(institutionId), Encode.forJava(productId));
        userApiRestClient._updateUserProductStatus(userId, institutionId, productId, OnboardedProductState.SUSPENDED, productRole);
        log.trace("suspend end");
    }

    @Override
    public User getUserById(String userId, String institutionId, List<String> fields) {
        log.trace("getUserById start");
        log.debug("getUserById id = {}", Encode.forJava(userId));
        String fieldsString = !CollectionUtils.isEmpty(fields) ? String.join(",", fields) : null;
        User user = userMapper.toUser(userApiRestClient._getUserDetailsById(userId, fieldsString, institutionId).getBody());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserById = {}", user);
        log.trace("getUserById end");
        return user;
    }

    @Override
    public User searchUserByFiscalCode(String fiscalCode, String institutionId) {
        log.trace("searchByFiscalCode start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "searchByFiscalCode fiscalCode = {}", Encode.forJava(fiscalCode));
        User user = userMapper.toUser(userApiRestClient._searchUserByFiscalCode(institutionId, SearchUserDto.builder().fiscalCode(fiscalCode).build()).getBody());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "searchByFiscalCode user = {}", user);
        log.trace("searchByFiscalCode end");
        return user;
    }

    @Override
    public void updateUser(String id, String institutionId, UpdateUserRequestDto userDto) {
        log.trace("updateUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "updateUser id = {}, institutionId = {}, userDto = {}", Encode.forJava(id), Encode.forJava(institutionId), userDto);
        Assert.notNull(id, "UUID is required");
        Assert.hasText(institutionId, "An institutionId is required");
        Assert.notNull(userDto, "A userDto is required");
        final Institution institution = institutionMapper.toInstitution(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null).getBody());
        if (Objects.isNull(institution)) {
            throw new ResourceNotFoundException("There is no institution for given institutionId");
        }
        userApiRestClient._updateUserRegistryAndSendNotification(id, institutionId, userMapper.toUpdateUserRequest(userDto));
        log.trace("updateUser end");
    }

    @Override
    public Collection<UserInfo> getUsersByInstitutionId(String institutionId, String productId, List<String> productRoles, List<String> roles, String loggedUserId) {
        log.trace("getUsersByInstitutionId start");
        log.debug("getUsersByInstitutionId institutionId = {}", Encode.forJava(institutionId));
        UserInfo.UserInfoFilter userInfoFilter = new UserInfo.UserInfoFilter();
        userInfoFilter.setProductId(productId);
        userInfoFilter.setProductRoles(productRoles);
        userInfoFilter.setAllowedStates(allowedStates);
        userInfoFilter.setRoles(roles);
        Collection<UserInfo> result = getUsers(institutionId, userInfoFilter, loggedUserId);
        log.info("getUsersByInstitutionId result size = {}", result.size());
        log.trace("getUsersByInstitutionId end");
        return result;
    }

    @Override
    public List<UserInstitutionRole> getAllUsersByInstitutionId(String institutionId, String productId, List<String> states, List<String> roles) {
        log.trace("getAllUsersByInstitutionId start");
        log.debug("getAllUsersByInstitutionId institutionId = {} with productId = {}, states = {}, roles = {}", Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(states != null ? states.toString() : null), Encode.forJava(roles != null ? roles.toString() : null));
        UserInfo.UserInfoFilter userInfoFilter = new UserInfo.UserInfoFilter();
        userInfoFilter.setProductId(productId);

        List<UserInstitutionRole> result = getAllUsers(institutionId, userInfoFilter)
                .stream()
                .map(userInstitution ->
                        userMapper.toUserInstitutionRole(userInstitution,
                                getLatestProduct(userInstitution)))
                .filter(userInstitutionRole -> CollectionUtils.isEmpty(states) || states.contains(userInstitutionRole.getStatus()))
                .filter(userInstitutionRole -> CollectionUtils.isEmpty(roles) || roles.contains(userInstitutionRole.getPartyRole()))
                .toList();

        log.info("getAllUsersByInstitutionId result size = {}", result.size());
        log.trace("getAllUsersByInstitutionId end");
        return result;
    }

    public Collection<UserInfo> getUsers(String institutionId, UserInfo.UserInfoFilter userInfoFilter, String loggedUserId) {
        log.trace("getUsers start");
        log.debug("getUsers institutionId = {}, userInfoFilter = {}", Encode.forJava(institutionId), userInfoFilter);

        Assert.hasText(institutionId, REQUIRED_INSTITUTION_ID_MESSAGE);

        return Optional.ofNullable(userApiRestClient._retrieveUsers(institutionId,
                                loggedUserId,
                                userInfoFilter.getUserId(),
                                userInfoFilter.getProductRoles(),
                                StringUtils.hasText(userInfoFilter.getProductId()) ? List.of(userInfoFilter.getProductId()) : null,
                                userInfoFilter.getRoles(),
                                !CollectionUtils.isEmpty(userInfoFilter.getAllowedStates()) ? userInfoFilter.getAllowedStates().stream().map(Enum::name).toList() : null)
                        .getBody())
                .map(userDataResponses -> userDataResponses.stream()
                        .map(userMapper::toUserInfo)
                        .toList())
                .orElse(Collections.emptyList());
    }

    public List<UserProductResponse> getAllUsers(String institutionId, UserInfo.UserInfoFilter userInfoFilter) {
        log.trace("getAllUsers start");
        log.debug("getAllUsers institutionId = {}, userInfoFilter = {}", Encode.forJava(institutionId), userInfoFilter);

        Assert.hasText(institutionId, REQUIRED_INSTITUTION_ID_MESSAGE);

        return Optional.ofNullable(userInstitutionApiRestClient._getInstitutionUsersUsingGET(institutionId,
                                Optional.ofNullable(userInfoFilter.getProductId()).map(List::of).orElse(null),
                                userInfoFilter.getRoles(),
                                userInfoFilter.getUserId())
                        .getBody())
                .orElse(Collections.emptyList());
    }

    @Override
    public String createUsers(String institutionId, String productId, UserToCreate userDto) {
        log.trace("createOrUpdateUserByFiscalCode start");
        log.debug("createOrUpdateUserByFiscalCode userDto = {}", userDto);
        Institution institution = verifyOnboardingStatus(institutionId, productId);
        OnboardedProduct validOnboarding = getValidOnboarding(institutionId, productId, institution);
        Product product = verifyProductPhasesAndRoles(productId, validOnboarding.getInstitutionType().toString(), userDto.getRole(), userDto.getProductRoles());
        List<CreateUserDto.Role> role = retrieveRole(product, userDto.getProductRoles(), userDto.getRole());
        String userId = createOrUpdateUserByFiscalCode(institution, productId, userDto, role);
        log.trace("createOrUpdateUserByFiscalCode end");
        return userId;
    }

    private static OnboardedProduct getValidOnboarding(String institutionId, String productId, Institution institution) {
        return institution.getOnboarding().stream()
                .filter(onb ->
                        productId.equals(onb.getProductId())
                                && ACTIVE.equals(onb.getStatus())
                )
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Institution %s not active for product %s",
                                institutionId, productId)));
    }

    private String createOrUpdateUserByFiscalCode(Institution institution, String productId, UserToCreate userDto, List<CreateUserDto.Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Role list cannot be empty");
        }

        it.pagopa.selfcare.user.generated.openapi.v1.dto.CreateUserDto createUserDto = buildCreateUserDto(institution, productId, userDto, roles);
        return userApiRestClient._createOrUpdateByFiscalCode(createUserDto).getBody();
    }

    private it.pagopa.selfcare.user.generated.openapi.v1.dto.CreateUserDto buildCreateUserDto(Institution institution, String productId, UserToCreate userDto, List<CreateUserDto.Role> roles) {
        var builder = it.pagopa.selfcare.user.generated.openapi.v1.dto.CreateUserDto.builder()
                .institutionId(institution.getId())
                .institutionDescription(institution.getDescription())
                .user(buildUser(userDto))
                .product(buildProduct(productId, roles, userDto.getToAddOnAggregates()));

        if (!Objects.isNull(institution.getRootParent())) {
            builder.institutionRootName(institution.getRootParent().getDescription());
        }

        return builder.build();
    }

    private it.pagopa.selfcare.user.generated.openapi.v1.dto.User buildUser(UserToCreate userDto) {
        return it.pagopa.selfcare.user.generated.openapi.v1.dto.User.builder()
                .fiscalCode(userDto.getTaxCode())
                .name(userDto.getName())
                .familyName(userDto.getSurname())
                .institutionEmail(userDto.getEmail())
                .build();
    }

    private Product1 buildProduct(String productId, List<CreateUserDto.Role> roles, Boolean toAddOnAggregates) {
        return Product1.builder()
                .productRoles(roles.stream().map(CreateUserDto.Role::getProductRole).toList())
                .role(roles.get(0).getPartyRole().name())
                .productId(productId)
                .toAddOnAggregates(toAddOnAggregates)
                .build();
    }

    @Override
    public void addUserProductRoles(String institutionId, String productId, String userId, Boolean toAddOnAggregates, Set<String> productRoles, String role) {
        log.trace("createOrUpdateUserByUserId start");
        log.debug("createOrUpdateUserByUserId userId = {}", Encode.forJava(userId));
        Institution institution = verifyOnboardingStatus(institutionId, productId);
        OnboardedProduct validOnboarding = getValidOnboarding(institutionId, productId, institution);
        PartyRole partyRole = Optional.ofNullable(role).map(PartyRole::valueOf).orElse(null);
        Product product = verifyProductPhasesAndRoles(productId, validOnboarding.getInstitutionType().toString(), partyRole, productRoles);
        List<CreateUserDto.Role> roleDto = retrieveRole(product, productRoles, partyRole);
        createOrUpdateUserByUserId(institution, productId, userId, roleDto, toAddOnAggregates);
        log.trace("createOrUpdateUserByUserId end");
    }

    @Override
    public UsersCountResponse getUserCount(String institutionId, String productId, List<String> roles, List<String> status) {
        log.trace("getUserCount start");
        UsersCountResponse userCount = userInstitutionApiRestClient._getUsersCount(institutionId, productId, roles, status).getBody();
        log.trace("getUserCount end");
        return userCount;
    }

    @Override
    public Boolean checkUser(String fiscalCode, String institutionId, String productId) {
        SearchUserDto searchUserDto = SearchUserDto
                .builder()
                .fiscalCode(fiscalCode)
                .build();
        return userInstitutionApiRestClient._checkUserUsingPOST(institutionId, productId, searchUserDto).getBody();
    }

    @Override
    public UserOtpEmailInfo getUserOtpEmailInfo(String userId) {
        return Optional.ofNullable(userApiRestClient._getUserOtpEmailInfo(userId))
                .filter(o -> o.getStatusCode().is2xxSuccessful())
                .map(o -> userMapper.toUserOtpEmailInfo(o.getBody()))
                .orElseThrow(() -> new ResourceNotFoundException("Unable to retrieve user otp info for userId: " + Encode.forJava(userId)));
    }

    private Institution verifyOnboardingStatus(String institutionId, String productId) {
        Institution institution = institutionMapper.toInstitution(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null).getBody());
        if (institution.getOnboarding() == null || institution.getOnboarding().stream()
                .noneMatch(onboarding -> onboarding.getProductId().equals(productId) && onboarding.getStatus().equals(RelationshipState.ACTIVE))
        ) {
            throw new InvalidOnboardingStatusException("The product is not active for the institution");
        } else return institution;
    }

    /**
     * This method is used to retrieve a list of roles for a given product.
     * It maps each product role to a CreateUserDto.Role object, which includes the label and party role.
     * To retrieve the party role, it uses the roleMappings of the product filtering by a white list of party roles (Only SUB_DELEGATE and OPERATOR are allowed
     * as Role to be assigned to a user in add Users ProductRoles operation).
     */
    private List<CreateUserDto.Role> retrieveRole(Product product, Set<String> productRoles, PartyRole partyRole) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProduct product = {}", product);
        return productRoles.stream().map(productRole -> {
            CreateUserDto.Role role = new CreateUserDto.Role();
            role.setProductRole(productRole);
            role.setLabel(ProductMapper.getLabel(productRole, product.getRoleMappings(null)).orElse(null));
            role.setPartyRole(partyRole);
            return role;
        }).toList();
    }

    private void createOrUpdateUserByUserId(Institution institution, String productId, String userId, List<CreateUserDto.Role> roles, Boolean toAddOnAggregates) {

        var addUserRoleDtoBuilder = AddUserRoleDto.builder()
                .institutionId(institution.getId())
                .institutionDescription(institution.getDescription())
                .product(it.pagopa.selfcare.user.generated.openapi.v1.dto.Product.builder()
                        .productRoles(roles.stream().map(CreateUserDto.Role::getProductRole).toList())
                        .role(roles.get(0).getPartyRole().name())
                        .productId(productId)
                        .toAddOnAggregates(toAddOnAggregates)
                        .build());

        if (!Objects.isNull(institution.getRootParent())) {
            addUserRoleDtoBuilder.institutionRootName(institution.getRootParent().getDescription());
        }
        AddUserRoleDto addUserRoleDto = addUserRoleDtoBuilder.build();
        userApiRestClient._createOrUpdateByUserId(userId, addUserRoleDto);
    }

    /**
     * <p>Get the product and verify if his phasesAdditionAllowed field allow to add users directly from dashboard with the specified partyRole and productRoles
     * or throw an exception.</p>
     *
     * <p>Dashboard can add the user directly if the phasesAdditionAllowed contains the "dashboard" string else additional steps
     * needs to be performed by the user (ex: sign additional documentation) and an onboarding procedure is required</p>
     *
     * <p>All the productRoles must be present in the roles of the ProductRoleInfo</p>
     *
     * @param productId product id
     * @param institutionType institution type
     * @param partyRole role to use
     * @param productRoles a set of productRoles
     * @return the product
     * @throws InvalidProductRoleException if dashboard can not add the user directly with the partyRole specified
     */
    private Product verifyProductPhasesAndRoles(String productId, String institutionType, PartyRole partyRole, Set<String> productRoles) {
        final Product product = productService.getProduct(productId);
        return Optional.ofNullable(partyRole)
                // If partyRole is present ==> get the ProductRoleInfo
                .map(pr -> product.getRoleMappings(institutionType).get(pr))
                // Check if phasesAdditionAllowed contains the "dashboard" string
                .filter(pri -> pri.getPhasesAdditionAllowed().stream().anyMatch(PHASE_ADDITION_ALLOWED.DASHBOARD.value::equals))
                // Obtain a set of validRoles from the product role info
                .map(pri -> pri.getRoles().stream().map(ProductRole::getCode).collect(Collectors.toSet()))
                // Check if all the productRoles in input are validRoles
                .filter(validRoles -> validRoles.containsAll(productRoles))
                // If all the previous filters are successful ==> Return the product
                .map(i -> product)
                // If any of the previous filters fail ==> throw exception
                .orElseThrow(() -> new InvalidProductRoleException("The product doesn't allow adding users directly with these role and productRoles"));
    }

    private OnboardedProductResponse getLatestProduct(UserProductResponse user) {
        if (user == null || user.getProducts() == null) {
            return null;
        }

        return user.getProducts().stream()
                .filter(p -> OnboardedProductState.ACTIVE.equals(p.getStatus()))
                .findFirst()
                .orElseGet(() ->
                        user.getProducts().stream()
                                .filter(p -> OnboardedProductState.SUSPENDED.equals(p.getStatus()))
                                .findFirst()
                                .orElseGet(() ->
                                        user.getProducts().stream()
                                                .filter(p -> p.getCreatedAt() != null)
                                                .max(Comparator.comparing(OnboardedProductResponse::getCreatedAt))
                                                .orElse(null)
                                )
                );
    }

}
