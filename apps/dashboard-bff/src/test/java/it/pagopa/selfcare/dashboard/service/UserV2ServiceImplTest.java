package it.pagopa.selfcare.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardedProductResponse;
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
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperImpl;
import it.pagopa.selfcare.dashboard.model.user.CreateUserDto;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.model.user.*;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.PHASE_ADDITION_ALLOWED;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.dashboard.model.institution.RelationshipState.*;
import static it.pagopa.selfcare.onboarding.common.PartyRole.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserV2ServiceImplTest extends BaseServiceTest {

    @InjectMocks
    private UserV2ServiceImpl userV2ServiceImpl;
    @Mock
    private UserGroupV2Service userGroupServiceMock;
    @Mock
    private UserApiRestClient userApiRestClient;
    @Mock
    private CoreInstitutionApiRestClient coreInstitutionApiRestClient;
    @Mock
    private ProductService productService;
    @Spy
    private InstitutionMapperImpl institutionMapper;
    @Spy
    private UserMapperImpl userMapper;
    @Mock
    private UserInstitutionApiRestClient userInstitutionApiRestClient;

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void getInstitutions() throws IOException {

        String userId = "userId";
        ClassPathResource resource = new ClassPathResource("expectations/InstitutionBase.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        Collection<InstitutionBase> expectedInstitutions = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        UserInfoResponse userInfoResponse = new UserInfoResponse();
        UserInstitutionRoleResponse inst = new UserInstitutionRoleResponse();
        inst.setInstitutionId("institutionBaseId");
        inst.setInstitutionName("institutionBaseName");
        inst.setRole("userRole");
        inst.setStatus(OnboardedProductState.ACTIVE);
        inst.setInstitutionRootName("parentDescription");

        userInfoResponse.setInstitutions(List.of(inst));


        when(userApiRestClient._getUserProductsInfo(userId, null, List.of(ACTIVE.name(), PENDING.name(), TOBEVALIDATED.name())))
                .thenReturn(ResponseEntity.ok(userInfoResponse));
        Collection<InstitutionBase> result = userV2ServiceImpl.getInstitutions(userId);
        Assertions.assertEquals(expectedInstitutions, result);
        Mockito.verify(userApiRestClient, Mockito.times(1))._getUserProductsInfo(userId, null, List.of(ACTIVE.name(), PENDING.name(), TOBEVALIDATED.name()));
    }

    @Test
    void getInstitutions_NotFound(){
        String userId = "userId";
        when(userApiRestClient._getUserProductsInfo(userId, null, List.of(ACTIVE.name(), PENDING.name(), TOBEVALIDATED.name())))
                .thenThrow(ResourceNotFoundException.class);
        Collection<InstitutionBase> result = userV2ServiceImpl.getInstitutions(userId);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getUserById() throws IOException {

        String userId = "userId";
        String institutionId = "institutionId";
        List<String> fields = new ArrayList<>();

        ClassPathResource resource = new ClassPathResource("expectations/User.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        User user = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        UserDetailResponse userDetailResponse = new UserDetailResponse();
        userDetailResponse.setId("123e4567-e89b-12d3-a456-426614174000");
        userDetailResponse.setFiscalCode("NLLGPJ67L30L783W");

        when(userApiRestClient._getUserDetailsById(userId, null, "institutionId")).thenReturn(ResponseEntity.ok(userDetailResponse));

        User result = userV2ServiceImpl.getUserById(userId, institutionId, fields);
        Assertions.assertEquals(user.getId(), result.getId());
        Assertions.assertEquals(user.getFiscalCode(), result.getFiscalCode());
        Mockito.verify(userApiRestClient, Mockito.times(1))._getUserDetailsById(userId, null, "institutionId");
    }

    @Test
    void searchUserByFiscalCodeNullReturn() {

        String fiscalCode = "fiscalCode";
        String institutionId = "institutionId";
        when(userApiRestClient._searchUserByFiscalCode(institutionId, SearchUserDto.builder().fiscalCode(fiscalCode).build())).thenReturn(ResponseEntity.ok().build());
        User result = userV2ServiceImpl.searchUserByFiscalCode(fiscalCode, institutionId);
        Assertions.assertNull(result);
    }

    @Test
    void searchUserByFiscalCode() throws IOException {

        String fiscalCode = "fiscalCode";
        String institutionId = "institutionId";
        ClassPathResource resource = new ClassPathResource("expectations/User.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        User user = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });
        UserDetailResponse userDetailResponse = new UserDetailResponse();
        userDetailResponse.setId("123e4567-e89b-12d3-a456-426614174000");
        userDetailResponse.setFiscalCode("NLLGPJ67L30L783W");
        when(userApiRestClient._searchUserByFiscalCode(institutionId, SearchUserDto.builder().fiscalCode(fiscalCode).build()))
                .thenReturn(ResponseEntity.ok(userDetailResponse));
        User result = userV2ServiceImpl.searchUserByFiscalCode(fiscalCode, institutionId);
        Assertions.assertEquals(user.getFiscalCode(), result.getFiscalCode());
        Assertions.assertEquals(user.getId(), result.getId());
        Mockito.verify(userApiRestClient, Mockito.times(1))._searchUserByFiscalCode(institutionId, SearchUserDto.builder().fiscalCode(fiscalCode).build());
    }

    @Test
    void getUsersByInstitutionIdWithoutInstitutionId() {
        String productId = "productId";
        List<String> productRoles = new ArrayList<>();
        String loggedUserId = "loggedUserId";

        Executable executable = () -> userV2ServiceImpl.getUsersByInstitutionId("", productId, productRoles, null, loggedUserId);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("An Institution id is required", e.getMessage());
    }

    @Test
    void getUsersByInstitutionIdEmptyUserInfo() {

        String institutionId = "institutionId";
        String productId = "productId";
        String loggedUserId = "loggedUserId";
        List<String> productRoles = new ArrayList<>();
        productRoles.add("productRole");

        UserInfo.UserInfoFilter userInfoFilter = new UserInfo.UserInfoFilter();
        userInfoFilter.setProductId(productId);
        userInfoFilter.setProductRoles(productRoles);

        when(userApiRestClient._retrieveUsers(institutionId, loggedUserId, null, productRoles, List.of(productId), null, null)).thenReturn(ResponseEntity.ok(new ArrayList<>()));

        Collection<UserInfo> result = userV2ServiceImpl.getUsersByInstitutionId(institutionId, productId, productRoles, null, loggedUserId);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getUsersByInstitutionId() throws IOException {

        String institutionId = "institutionId";
        String productId = "productId";
        List<String> productRoles = new ArrayList<>();
        productRoles.add("productRole");
        String loggedUserId = "loggedUserId";

        UserInfo.UserInfoFilter userInfoFilter = new UserInfo.UserInfoFilter();
        userInfoFilter.setProductId(productId);
        userInfoFilter.setProductRoles(productRoles);

        ClassPathResource resource = new ClassPathResource("expectations/CollectionUserInfo.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        Collection<UserInfo> userInfo = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        UserDataResponse user = new UserDataResponse();
        user.setRole("MANAGER");

        when(userApiRestClient._retrieveUsers(institutionId, loggedUserId, null, productRoles, List.of(productId), null, null))
                .thenReturn(ResponseEntity.ok(List.of(user)));

        Collection<UserInfo> result = userV2ServiceImpl.getUsersByInstitutionId(institutionId, productId, productRoles, null, loggedUserId);
        Assertions.assertEquals(userInfo.size(), result.size());
        Mockito.verify(userApiRestClient, Mockito.times(1))._retrieveUsers(institutionId, loggedUserId, null, productRoles, List.of(productId), null, null);
    }


    @Test
    void getAllUsersByInstitutionIdWithoutInstitutionId() {
        String productId = "productId";
        List<String> roles = new ArrayList<>();

        Executable executable = () -> userV2ServiceImpl.getAllUsersByInstitutionId("", productId, null, roles);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("An Institution id is required", e.getMessage());
    }

    @Test
    void getAllUsersByInstitutionIdEmptyUserInfo() {

        String institutionId = "institutionId";
        String productId = "productId";
        List<String> roles = List.of("MANAGER");
        List<String> states = List.of("ACTIVE", "DELETED");

        when(userInstitutionApiRestClient._getInstitutionUsersUsingGET(institutionId, List.of(productId), null, null)).thenReturn(ResponseEntity.ok(new ArrayList<>()));

        Collection<UserInstitutionRole> result = userV2ServiceImpl.getAllUsersByInstitutionId(institutionId, productId, states, roles);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getAllUsersByInstitutionId() {

        String institutionId = "institutionId";
        String productId = "prod-io";
        List<String> roles = List.of("MANAGER");
        List<String> states = List.of("ACTIVE", "DELETED");

        it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse product1 = new it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse();
        product1.setRole("OPERATOR");
        product1.setProductId("prod-io");
        product1.setStatus(OnboardedProductState.ACTIVE);
        product1.setCreatedAt(LocalDateTime.now().minusDays(2));
        it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse product2 = new it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse();
        product2.setRole("MANAGER");
        product2.setProductId("prod-io");
        product2.setStatus(OnboardedProductState.DELETED);
        product2.setCreatedAt(LocalDateTime.now());

        UserProductResponse userProductResponse1 = new UserProductResponse();
        userProductResponse1.setId("userId1");
        userProductResponse1.setName("name1");
        userProductResponse1.setSurname("surname1");
        userProductResponse1.setTaxCode("taxCode1");
        userProductResponse1.setEmail("email1");
        userProductResponse1.setProducts(List.of(product1,product2));

        UserProductResponse userProductResponse2 = new UserProductResponse();
        userProductResponse2.setId("userId2");
        userProductResponse2.setName("name2");
        userProductResponse2.setSurname("surname2");
        userProductResponse2.setTaxCode("taxCode2");
        userProductResponse2.setEmail("email2");
        userProductResponse2.setProducts(List.of(product2));

        UserInstitutionRole expectedResponse = new UserInstitutionRole();
        expectedResponse.setId(userProductResponse2.getId());
        expectedResponse.setName(userProductResponse2.getName());
        expectedResponse.setSurname(userProductResponse2.getSurname());
        expectedResponse.setFiscalCode(userProductResponse2.getTaxCode());
        expectedResponse.setEmail(userProductResponse2.getEmail());
        expectedResponse.setStatus(product2.getStatus().name());
        expectedResponse.setPartyRole(product2.getRole());


        when(userInstitutionApiRestClient._getInstitutionUsersUsingGET(institutionId, List.of(productId), null, null))
                .thenReturn(ResponseEntity.ok(List.of(userProductResponse1, userProductResponse2)));

        Collection<UserInstitutionRole> result = userV2ServiceImpl.getAllUsersByInstitutionId(institutionId, productId, states, roles);
        Assertions.assertEquals(List.of(expectedResponse), result.stream().toList());
        Mockito.verify(userInstitutionApiRestClient, Mockito.times(1))._getInstitutionUsersUsingGET(institutionId, List.of(productId), null, null);
    }

    @Test
    void suspend() {

        String userId = "rel1";
        String institutionid = "id1";
        String productId = "prod-pagopa";
        String productRole = "admin";

        userV2ServiceImpl.suspendUserProduct(userId, institutionid, productId, productRole);
        verify(userApiRestClient, times(1))
                ._updateUserProductStatus(userId, institutionid, productId, OnboardedProductState.SUSPENDED, productRole);
        Mockito.verifyNoMoreInteractions(userApiRestClient);
    }


    @Test
    void activate() {

        String userId = "rel1";
        String institutionid = "id1";
        String productId = "prod-pagopa";
        String productRole = "admin";

        userV2ServiceImpl.activateUserProduct(userId, institutionid, productId, productRole);

        Mockito.verify(userApiRestClient, Mockito.times(1))
                ._updateUserProductStatus(userId, institutionid, productId, OnboardedProductState.ACTIVE, productRole);
        Mockito.verifyNoMoreInteractions(userApiRestClient);
    }

    @Test
    void delete() {

        String userId = "rel1";
        String institutionid = "id1";
        String productId = "prod-pagopa";
        String productRole = "admin";

        userV2ServiceImpl.deleteUserProduct(userId, institutionid, productId, productRole);

        Mockito.verify(userApiRestClient, Mockito.times(1))
                ._updateUserProductStatus(userId, institutionid, productId, OnboardedProductState.DELETED, productRole);
        Mockito.verify(userGroupServiceMock, Mockito.times(1))
                .deleteMembersByUserId(userId, institutionid, productId);
        Mockito.verifyNoMoreInteractions(userApiRestClient, userGroupServiceMock);
    }

    @Test
    void updateUser() {
        final String institutionId = "institutionId";
        final String userId = "userId";
        final UpdateUserRequestDto user = mockInstance(new UpdateUserRequestDto());
        Institution institutionMock = mockInstance(new Institution());

        InstitutionResponse institution = new InstitutionResponse();
        institutionMock.setId(institutionId);

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));

        Executable executable = () -> userV2ServiceImpl.updateUser(userId, institutionId, user);

        assertDoesNotThrow(executable);
        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
        verify(userApiRestClient, times(1))._updateUserRegistryAndSendNotification(eq(userId), eq(institutionId), any(UpdateUserRequest.class));
        verifyNoMoreInteractions(userApiRestClient);
    }

    @Test
    void updateUser_nullInstitution() {
        final String institutionId = "institutionId";
        final String userId = "userId";
        final UpdateUserRequestDto user = mockInstance(new UpdateUserRequestDto());
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok().build());
        Executable executable = () -> userV2ServiceImpl.updateUser(userId, institutionId, user);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals("There is no institution for given institutionId", exception.getMessage());
        verifyNoInteractions(userApiRestClient);
    }

    @Test
    void addUserProductRoles_ok() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";
        Set<String> productRoles = new HashSet<>(List.of("operator"));
        String role = "OPERATOR";
        Boolean toAddOnAggregates = true;
        Product product = getProduct();

        Institution institution = new Institution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(RelationshipState.ACTIVE);
        institution.setOnboarding(List.of(onboardedProduct));

        CreateUserDto.Role roleDto = new CreateUserDto.Role();
        roleDto.setProductRole("operator");
        roleDto.setLabel("operator");
        roleDto.setPartyRole(OPERATOR);

        AddUserRoleDto addRoleDto = new AddUserRoleDto();
        addRoleDto.setProduct(it.pagopa.selfcare.user.generated.openapi.v1.dto.Product.builder().productId(productId).build());
        addRoleDto.setInstitutionId(institutionId);

        InstitutionResponse institutionMock = new InstitutionResponse();
        institutionMock.setId(institutionId);
        OnboardedProductResponse onb = new OnboardedProductResponse();
        onb.setProductId(productId);
        onb.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onb.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.PA);

        institutionMock.setOnboarding(List.of(onb));

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionMock));
        when(productService.getProduct(productId)).thenReturn(product);

        userV2ServiceImpl.addUserProductRoles(institutionId, productId, userId, toAddOnAggregates, productRoles, role);

        verify(userApiRestClient, times(1))._createOrUpdateByUserId(anyString(), any());
        verifyNoMoreInteractions(userApiRestClient);
    }

    @Test
    void addUserProductRoles_ok_withoutPartyRole() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";
        Set<String> productRoles = new HashSet<>(List.of("operator"));
        Boolean toAddOnAggregates = true;
        Product product = getProduct();

        Institution institution = new Institution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(RelationshipState.ACTIVE);
        institution.setOnboarding(List.of(onboardedProduct));

        AddUserRoleDto addRoleDto = new AddUserRoleDto();
        addRoleDto.setProduct(it.pagopa.selfcare.user.generated.openapi.v1.dto.Product.builder().productId(productId).build());
        addRoleDto.setInstitutionId(institutionId);

        InstitutionResponse institutionMock = new InstitutionResponse();
        institutionMock.setId(institutionId);
        OnboardedProductResponse onb = new OnboardedProductResponse();
        onb.setProductId(productId);
        onb.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onb.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.GPU);

        institutionMock.setOnboarding(List.of(onb));

        CreateUserDto.Role roleDto = new CreateUserDto.Role();
        roleDto.setProductRole("operator");
        roleDto.setLabel("operator");
        roleDto.setPartyRole(OPERATOR);

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionMock));
        when(productService.getProduct(productId)).thenReturn(product);

        userV2ServiceImpl.addUserProductRoles(institutionId, productId, userId, toAddOnAggregates, productRoles, OPERATOR.name());

        verify(userApiRestClient, times(1))._createOrUpdateByUserId(anyString(), any());
        verifyNoMoreInteractions(userApiRestClient);
    }

    @Test
    void addUserProductRoles_invalidOnboardingStatus() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";
        Set<String> productRoles = new HashSet<>(List.of("operator"));
        String role = "MANAGER";
        Boolean toAddOnAggregates = true;

        InstitutionResponse institution = new InstitutionResponse();
        institution.setId(institutionId);

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));

        Assertions.assertThrows(InvalidOnboardingStatusException.class, () -> userV2ServiceImpl.addUserProductRoles(institutionId, productId, userId, toAddOnAggregates, productRoles, role));

        verifyNoInteractions(userApiRestClient);
    }

    @Test
    void addUserProductRolesWithInvalidPartyRole() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";
        final Set<String> productRoles = Set.of("operator");
        Boolean toAddOnAggregates = true;
        final Product product = getProduct();

        final InstitutionResponse institution = new InstitutionResponse();
        final OnboardedProductResponse onboardedProduct = new OnboardedProductResponse();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onboardedProduct.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.GSP);
        institution.setOnboarding(List.of(onboardedProduct));

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));
        when(productService.getProduct(productId)).thenReturn(product);

        assertThrows(InvalidProductRoleException.class,
                () -> userV2ServiceImpl.addUserProductRoles(institutionId, productId, userId, toAddOnAggregates, productRoles, null),
                "The product doesn't allow adding users directly with these role and productRoles");

        assertThrows(InvalidProductRoleException.class,
                () -> userV2ServiceImpl.addUserProductRoles(institutionId, productId, userId, toAddOnAggregates, productRoles, "MANAGER"),
                "The product doesn't allow adding users directly with these role and productRoles");
    }

    @Test
    void addUserProductRolesWithInvalidPhasesAdditionAllowed() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";
        final Set<String> productRoles = Set.of("manager");
        final String role = "MANAGER";
        Boolean toAddOnAggregates = true;

        final Product product = getProduct();
        final ProductRoleInfo productRoleInfoManager = new ProductRoleInfo();
        productRoleInfoManager.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.DASHBOARD_ASYNC.value, PHASE_ADDITION_ALLOWED.ONBOARDING.value));
        final ProductRole pr = new ProductRole();
        pr.setCode("manager");
        pr.setLabel("manager");
        productRoleInfoManager.setRoles(List.of(pr));
        product.setRoleMappings(Map.of(MANAGER, productRoleInfoManager));

        final InstitutionResponse institution = new InstitutionResponse();
        final OnboardedProductResponse onboardedProduct = new OnboardedProductResponse();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onboardedProduct.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.PA);
        institution.setOnboarding(List.of(onboardedProduct));

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));
        when(productService.getProduct(productId)).thenReturn(product);

        assertThrows(InvalidProductRoleException.class,
                () -> userV2ServiceImpl.addUserProductRoles(institutionId, productId, userId, toAddOnAggregates, productRoles, role),
                "The product doesn't allow adding users directly with these role and productRoles");
    }

    @Test
    void addUserProductRolesWithInvalidProductRoles() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";
        final Set<String> productRoles = Set.of("operator2", "operator0", "operator1");
        final String role = "OPERATOR";
        Boolean toAddOnAggregates = true;

        final Product product = getProduct();
        final ProductRoleInfo productRoleInfoOperator = getProductRoleInfo();
        product.setRoleMappings(Map.of(PartyRole.OPERATOR, productRoleInfoOperator));

        final InstitutionResponse institution = new InstitutionResponse();
        final OnboardedProductResponse onboardedProduct = new OnboardedProductResponse();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onboardedProduct.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.PT);
        institution.setOnboarding(List.of(onboardedProduct));

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));
        when(productService.getProduct(productId)).thenReturn(product);

        assertThrows(InvalidProductRoleException.class,
                () -> userV2ServiceImpl.addUserProductRoles(institutionId, productId, userId, toAddOnAggregates, productRoles, role),
                "The product doesn't allow adding users directly with these role and productRoles");
    }

    private static ProductRoleInfo getProductRoleInfo() {
        final ProductRoleInfo productRoleInfoOperator = new ProductRoleInfo();
        productRoleInfoOperator.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.DASHBOARD_ASYNC.value, PHASE_ADDITION_ALLOWED.DASHBOARD.value));
        final ProductRole pr1 = new ProductRole();
        pr1.setCode("operator1");
        pr1.setLabel("operator1");
        final ProductRole pr2 = new ProductRole();
        pr2.setCode("operator2");
        pr2.setLabel("operator2");
        productRoleInfoOperator.setRoles(List.of(pr1, pr2));
        return productRoleInfoOperator;
    }

    @Test
    void createUsersByFiscalCode() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String productRole = "operator";
        UserToCreate userToCreate = new UserToCreate();
        HashSet<String> productRoles = new HashSet<>();
        productRoles.add(productRole);
        userToCreate.setRole(OPERATOR);
        userToCreate.setProductRoles(productRoles);
        userToCreate.setToAddOnAggregates(true);

        Product product = getProduct();

        Institution institution = new Institution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(RelationshipState.ACTIVE);
        institution.setOnboarding(List.of(onboardedProduct));

        InstitutionResponse institutionMock = new InstitutionResponse();
        institution.setId(institutionId);
        OnboardedProductResponse onb = new OnboardedProductResponse();
        onb.setProductId(productId);
        onb.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onb.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.PRV);

        institutionMock.setOnboarding(List.of(onb));

        CreateUserDto.Role roleDto = new CreateUserDto.Role();
        roleDto.setProductRole("operator");
        roleDto.setLabel("operator");
        roleDto.setPartyRole(OPERATOR);

        when(productService.getProduct(productId)).thenReturn(product);
        when(userApiRestClient._createOrUpdateByFiscalCode(any())).thenReturn(ResponseEntity.ok("userId"));
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionMock));

        String userId = userV2ServiceImpl.createUsers(institutionId, productId, userToCreate);

        assertNotNull(userId);

        verify(userApiRestClient, times(1))._createOrUpdateByFiscalCode(any());
        verifyNoMoreInteractions(userApiRestClient);
    }

    @Test
    void createUsersByFiscalCodeWithPartyRole() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String productRole = "operator";
        UserToCreate userToCreate = new UserToCreate();
        HashSet<String> productRoles = new HashSet<>();
        productRoles.add(productRole);
        userToCreate.setRole(OPERATOR);
        userToCreate.setProductRoles(productRoles);
        userToCreate.setToAddOnAggregates(true);

        Product product = getProduct();

        Institution institution = new Institution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(RelationshipState.ACTIVE);
        institution.setOnboarding(List.of(onboardedProduct));

        CreateUserDto.Role roleDto = new CreateUserDto.Role();
        roleDto.setProductRole("operator");
        roleDto.setLabel("operator");
        roleDto.setPartyRole(OPERATOR);

        InstitutionResponse institutionMock = new InstitutionResponse();
        institutionMock.setId(institutionId);

        OnboardedProductResponse onboardedProductResponse = new OnboardedProductResponse();
        onboardedProductResponse.setProductId(productId);
        onboardedProductResponse.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onboardedProductResponse.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.PA);

        institutionMock.setOnboarding(List.of(onboardedProductResponse));

        when(productService.getProduct(productId)).thenReturn(product);
        when(userApiRestClient._createOrUpdateByFiscalCode(any())).thenReturn(ResponseEntity.ok("userId"));
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionMock));

        String userId = userV2ServiceImpl.createUsers(institutionId, productId, userToCreate);

        assertNotNull(userId);

        verify(userApiRestClient, times(1))._createOrUpdateByFiscalCode(any());
        verifyNoMoreInteractions(userApiRestClient);
    }

    @Test
    void createUsersByFiscalCodeWithOnboardingNotActive() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        UserToCreate userToCreate = new UserToCreate();
        HashSet<String> productRoles = new HashSet<>();
        productRoles.add("operator");
        userToCreate.setProductRoles(productRoles);
        userToCreate.setToAddOnAggregates(true);

        InstitutionResponse institutionMock = new InstitutionResponse();
        institutionMock.setId(institutionId);

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionMock));

        assertThrows(InvalidOnboardingStatusException.class, () -> userV2ServiceImpl.createUsers(institutionId, productId, userToCreate));

        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
        verifyNoMoreInteractions(userApiRestClient);
    }

    @Test
    void createUsersByFiscalCodeWithInvalidPartyRole() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final UserToCreate userToCreate = new UserToCreate();
        final Set<String> productRoles = Set.of("manager");
        userToCreate.setRole(MANAGER);
        userToCreate.setProductRoles(productRoles);
        userToCreate.setToAddOnAggregates(true);

        final Product product = getProduct();

        final InstitutionResponse institution = new InstitutionResponse();
        final OnboardedProductResponse onboardedProduct = new OnboardedProductResponse();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onboardedProduct.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.PRV);
        institution.setOnboarding(List.of(onboardedProduct));

        when(productService.getProduct(productId)).thenReturn(product);
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));

        assertThrows(InvalidProductRoleException.class,
                () -> userV2ServiceImpl.createUsers(institutionId, productId, userToCreate),
                "The product doesn't allow adding users directly with these role and productRoles");
    }

    @Test
    void createUsersByFiscalCodeWithInvalidPhasesAdditionAllowed() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final UserToCreate userToCreate = new UserToCreate();
        final Set<String> productRoles = Set.of("manager");
        userToCreate.setRole(MANAGER);
        userToCreate.setProductRoles(productRoles);
        userToCreate.setToAddOnAggregates(true);

        final Product product = getProduct();
        final ProductRoleInfo productRoleInfoManager = new ProductRoleInfo();
        productRoleInfoManager.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.DASHBOARD_ASYNC.value, PHASE_ADDITION_ALLOWED.ONBOARDING.value));
        final ProductRole pr = new ProductRole();
        pr.setCode("manager");
        pr.setLabel("manager");
        productRoleInfoManager.setRoles(List.of(pr));
        product.setRoleMappings(Map.of(MANAGER, productRoleInfoManager));

        final InstitutionResponse institution = new InstitutionResponse();
        final OnboardedProductResponse onboardedProduct = new OnboardedProductResponse();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onboardedProduct.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.PA);
        institution.setOnboarding(List.of(onboardedProduct));

        when(productService.getProduct(productId)).thenReturn(product);
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));

        assertThrows(InvalidProductRoleException.class,
                () -> userV2ServiceImpl.createUsers(institutionId, productId, userToCreate),
                "The product doesn't allow adding users directly with these role and productRoles");
    }

    @Test
    void createUsersByFiscalCodeWithInvalidProductRoles() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final UserToCreate userToCreate = new UserToCreate();
        final Set<String> productRoles = Set.of("operator2", "operator0", "operator1");
        userToCreate.setRole(OPERATOR);
        userToCreate.setProductRoles(productRoles);
        userToCreate.setToAddOnAggregates(true);

        final Product product = getProduct();
        final ProductRoleInfo productRoleInfoOperator = new ProductRoleInfo();
        productRoleInfoOperator.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.DASHBOARD_ASYNC.value, PHASE_ADDITION_ALLOWED.DASHBOARD.value));
        final ProductRole pr1 = new ProductRole();
        pr1.setCode("operator1");
        pr1.setLabel("operator1");
        final ProductRole pr2 = new ProductRole();
        pr2.setCode("operator2");
        pr2.setLabel("operator2");
        productRoleInfoOperator.setRoles(List.of(pr1, pr2));
        product.setRoleMappings(Map.of(PartyRole.OPERATOR, productRoleInfoOperator));

        final InstitutionResponse institution = new InstitutionResponse();
        final OnboardedProductResponse onboardedProduct = new OnboardedProductResponse();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onboardedProduct.setInstitutionType(OnboardedProductResponse.InstitutionTypeEnum.PA);
        institution.setOnboarding(List.of(onboardedProduct));

        when(productService.getProduct(productId)).thenReturn(product);
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));

        assertThrows(InvalidProductRoleException.class,
                () -> userV2ServiceImpl.createUsers(institutionId, productId, userToCreate),
                "The product doesn't allow adding users directly with these role and productRoles");
    }

    @Test
    void getUserCount() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<String> roles = List.of(MANAGER.name(), DELEGATE.name());
        final List<String> status = List.of(PENDING.name(), ACTIVE.name());
        final UsersCountResponse userCount = getUsersCountResponse();
        when(userInstitutionApiRestClient._getUsersCount(institutionId, productId, roles, status)).thenReturn(ResponseEntity.ok(userCount));

        assertEquals(userCount, userV2ServiceImpl.getUserCount(institutionId, productId, roles, status));
        verify(userInstitutionApiRestClient, times(1))
                ._getUsersCount(institutionId, productId, roles, status);
        verifyNoMoreInteractions(userInstitutionApiRestClient);
    }

    @Test
    void checkUser() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String fiscalCode = "fiscalCode";
        SearchUserDto userDto = new SearchUserDto(fiscalCode);

        when(userInstitutionApiRestClient._checkUserUsingPOST(institutionId, productId, userDto)).thenReturn(ResponseEntity.ok(Boolean.TRUE));

        assertEquals(Boolean.TRUE, userV2ServiceImpl.checkUser(fiscalCode, institutionId, productId));
        verify(userInstitutionApiRestClient, times(1))
                ._checkUserUsingPOST(institutionId, productId,userDto);
        verifyNoMoreInteractions(userInstitutionApiRestClient);
    }

    @Test
    void getUserOtpEmailInfoTest() {
        when(userApiRestClient._getUserOtpEmailInfo(anyString())).thenReturn(ResponseEntity.ok(UserOtpEmailInfoResponse.builder()
                .userId("userId").otpReferenceInstitutionId("institutionId").otpEmail("email").canUserChangeOtpEmail(true).build()));

        final UserOtpEmailInfo response = userV2ServiceImpl.getUserOtpEmailInfo("userId");

        assertEquals("userId", response.getUserId());
        assertEquals("email", response.getOtpEmail());
        assertEquals("institutionId", response.getOtpReferenceInstitutionId());
        assertTrue(response.getCanUserChangeOtpEmail());
    }

    private static UsersCountResponse getUsersCountResponse() {
        final List<it.pagopa.selfcare.user.generated.openapi.v1.dto.PartyRole> expectedRoles = List.of(it.pagopa.selfcare.user.generated.openapi.v1.dto.PartyRole.MANAGER, it.pagopa.selfcare.user.generated.openapi.v1.dto.PartyRole.DELEGATE);
        final List<OnboardedProductState> expectedStatus = List.of(OnboardedProductState.PENDING, OnboardedProductState.ACTIVE);

        final UsersCountResponse userCount = new UsersCountResponse();
        userCount.setInstitutionId("institutionId");
        userCount.setProductId("productId");
        userCount.setRoles(expectedRoles);
        userCount.setStatus(expectedStatus);
        userCount.setCount(2L);
        return userCount;
    }

    private static Product getProduct() {
        Product product = new Product();
        Map<PartyRole, ProductRoleInfo> map = new EnumMap<>(PartyRole.class);

        ProductRoleInfo productRoleInfoOperator = new ProductRoleInfo();
        productRoleInfoOperator.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.DASHBOARD.value));
        ProductRole productRole = new ProductRole();
        productRole.setCode("operator");
        productRole.setLabel("operator");
        productRoleInfoOperator.setRoles(List.of(productRole));
        map.put(OPERATOR, productRoleInfoOperator);

        ProductRoleInfo productRoleInfoDelegate = new ProductRoleInfo();
        productRoleInfoDelegate.setPhasesAdditionAllowed(List.of(PHASE_ADDITION_ALLOWED.DASHBOARD.value));
        ProductRole productRole2 = new ProductRole();
        productRole2.setCode("delegate");
        productRole2.setLabel("delegate");
        productRoleInfoDelegate.setRoles(List.of(productRole2));
        map.put(PartyRole.DELEGATE, productRoleInfoDelegate);

        product.setRoleMappings(map);
        return product;
    }
}
