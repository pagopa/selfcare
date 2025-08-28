package it.pagopa.selfcare.user.service;


import com.microsoft.applicationinsights.TelemetryClient;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.constraint.Assert;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.controller.request.AddUserRoleDto;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.controller.response.*;
import it.pagopa.selfcare.user.controller.response.product.OnboardedProductWithActions;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.exception.UserRoleAlreadyPresentException;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.UserToNotify;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import it.pagopa.selfcare.user.service.utils.CreateOrUpdateUserByFiscalCodeResponse;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.bson.types.ObjectId;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.openapi.quarkus.user_registry_json.model.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static it.pagopa.selfcare.onboarding.common.PartyRole.*;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.user.constant.CustomError.*;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.EVENTS_USER_INSTITUTION_SUCCESS;
import static it.pagopa.selfcare.user.model.constants.EventsName.EVENT_USER_MS_NAME;
import static it.pagopa.selfcare.user.model.constants.OnboardedProductState.*;
import static it.pagopa.selfcare.user.service.UserServiceImpl.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class UserServiceTest {

    private static final UserResource userResource;
    private static final UUID userId = UUID.randomUUID();
    private static final String workContractsKey = "userMailUuid";

    static {
        userResource = new UserResource();
        userResource.setId(userId);
        NameCertifiableSchema certifiedName = new NameCertifiableSchema();
        FamilyNameCertifiableSchema certifiedSurname = new FamilyNameCertifiableSchema();
        certifiedSurname.setValue("surname");
        certifiedName.setValue("name");
        userResource.setName(certifiedName);
        userResource.setFamilyName(certifiedSurname);
        userResource.setFiscalCode("taxCode");
        org.openapi.quarkus.user_registry_json.model.EmailCertifiableSchema certifiedEmail = new EmailCertifiableSchema();
        certifiedEmail.setValue("test@test.it");
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(certifiedEmail);
        userResource.setEmail(certifiedEmail);

        Map<String, WorkContactResource> map = new HashMap<>();
        map.put(workContractsKey, workContactResource);
        userResource.setWorkContacts(map);


    }

    @Inject
    UserService userService;
    @InjectMock
    private UserInstitutionService userInstitutionService;
    @InjectMock
    private UserNotificationService userNotificationService;
    @InjectMock
    private UserInfoService userInfoService;
    @InjectMock
    private UserRegistryService userRegistryApi;
    @Spy
    private UserMapper userMapper;
    @InjectMock
    private ProductService productService;
    @InjectMock
    private UserUtils userUtils;
    @InjectMock
    private TelemetryClient telemetryClient;

    private static CreateUserDto getCreateUserDto(String productId, String fiscalCode, String institutionId, List<String> productRoles, String role) {

        CreateUserDto.Product product = new CreateUserDto.Product();
        product.setProductId(productId);
        product.setProductRoles(productRoles);
        product.setRole(role);

        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode(fiscalCode);
        user.setInstitutionEmail("user@example.com");

        CreateUserDto userDto = new CreateUserDto();
        userDto.setUser(user);
        userDto.setInstitutionId(institutionId);
        userDto.setProduct(product);
        userDto.setHasToSendEmail(false);
        return userDto;
    }

    private UserInstitution createUserInstitution() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId(userId.toString());
        userInstitution.setInstitutionId("institutionId");
        userInstitution.setUserMailUuid(workContractsKey);
        userInstitution.setInstitutionRootName("institutionRootName");

        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        product.setProductRole("admin");
        product.setRole(MANAGER);
        product.setStatus(OnboardedProductState.ACTIVE);

        OnboardedProduct productTest = new OnboardedProduct();
        productTest.setProductId("prod-test");
        productTest.setProductRole("operator");
        productTest.setRole(PartyRole.OPERATOR);
        productTest.setStatus(OnboardedProductState.DELETED);
        List<OnboardedProduct> products = new ArrayList<>();
        products.add(product);
        products.add(productTest);
        userInstitution.setProducts(products);
        return userInstitution;
    }

    private UserInstitution createUserInstitution_ADMIN_EA_IO() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId(userId.toString());
        userInstitution.setInstitutionId("institutionId");
        userInstitution.setUserMailUuid(workContractsKey);
        userInstitution.setInstitutionRootName("institutionRootName");

        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("prod-io");
        product.setProductRole("admin");
        product.setRole(ADMIN_EA);
        product.setStatus(OnboardedProductState.ACTIVE);

        List<OnboardedProduct> products = new ArrayList<>();
        products.add(product);
        userInstitution.setProducts(products);
        return userInstitution;
    }

    @Test
    void getUsersEmailsTest() {

        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<List<String>> subscriber = userService
                .getUsersEmails("institutionId", "productId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();

        verify(userRegistryApi).findByIdUsingGET(anyString(), anyString());
        verify(userInstitutionService).findAllWithFilter(any());
    }

    @Test
    void getUsersEmailsTestWithNullUserMailUUid() {

        UserInstitution userInstitution = createUserInstitution();
        userInstitution.setUserMailUuid(null);
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(createUserInstitution()));

        when(userRegistryApi.findByIdUsingGET(anyString(), eq(userInstitution.getUserId())))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<List<String>> subscriber = userService
                .getUsersEmails("institutionId", "productId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();

        verify(userRegistryApi).findByIdUsingGET(anyString(), eq(userInstitution.getUserId()));
        verify(userInstitutionService).findAllWithFilter(any());
    }

    @Test
    void getUserById() {
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any()))
                .thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<UserDetailResponse> subscriber = userService
                .getUserById(userId.toString(), "institutionId", null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();

    }

    @Test
    void getUserByIdNotFound() {
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().nullItem());
        UniAssertSubscriber<UserDetailResponse> subscriber = userService
                .getUserById(userId.toString(), "institutionId", null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();

    }

    @Test
    void sendEventsByDateAndUserIdAndInstitutionId() {
        final Integer page = 1;
        final String institutionId = "institutionId";
        final String userId = "userId";
        final OffsetDateTime fromDate = OffsetDateTime.now();
        final UserInstitution userInstitution = createUserInstitution();
        userInstitution.getProducts().forEach(onboardedProduct -> onboardedProduct.setCreatedAt(fromDate.plusDays(1)));
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        when(userInstitutionService.pageCountUserInstitutionsAfterDateWithFilter(anyMap(), any()))
                .thenReturn(Uni.createFrom().item(page));
        when(userInstitutionService.findUserInstitutionsAfterDateWithFilter(anyMap(), any(), eq(0)))
                .thenReturn(Multi.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        userService
                .sendEventsByDateAndUserIdAndInstitutionId(fromDate, institutionId, userId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create()).awaitItem();

        // Verify the result
        verify(userInstitutionService, times(1))
                .findUserInstitutionsAfterDateWithFilter(anyMap(), any(), eq(0));
        verify(userRegistryApi, times(1))
                .findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userId);
        ArgumentCaptor<Map<String, Double>> metricsName = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient, times(1)).trackEvent(eq(EVENT_USER_MS_NAME), any(), metricsName.capture());
        assertEquals(EVENTS_USER_INSTITUTION_SUCCESS, metricsName.getValue().keySet().stream().findFirst().orElse(null));
    }

    @Test
    void sendOldData_noFilters() {
        final OffsetDateTime fromDate = OffsetDateTime.now();
        final UserInstitution userInstitution = createUserInstitution();
        userInstitution.getProducts().forEach(onboardedProduct -> onboardedProduct.setCreatedAt(fromDate.plusDays(1)));
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        when(userInstitutionService.findUserInstitutionsAfterDateWithFilter(anyMap(), any())).thenReturn(Multi.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<Void> subscriber = userService
                .sendEventsByDateAndUserIdAndInstitutionId(fromDate, null, null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.assertCompleted();
    }

    @Test
    void getUserProductsByInstitutionTest() {
        final UserInstitution userInstitution = createUserInstitution();
        final UserInstitution userInstitutionWithoutMail = createUserInstitution();
        userInstitutionWithoutMail.setUserMailUuid(null);

        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().items(userInstitution, userInstitutionWithoutMail));
        when(UserInstitution.find(any(), (Object) any()))
                .thenReturn(query);
        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));
        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userInstitutionWithoutMail.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));

        AssertSubscriber<UserProductResponse> subscriber = userService
                .getUserProductsByInstitution(userInstitution.getInstitutionId())
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        List<UserProductResponse> actual = subscriber.assertCompleted().getItems();
        assertNotNull(actual);
        assertEquals(2, actual.size());

        UserProductResponse actualUser = actual.get(0);
        assertEquals(userResource.getWorkContacts().get(workContractsKey).getEmail().getValue(), actualUser.getEmail());
        assertEquals(userResource.getName().getValue(), actualUser.getName());
        assertEquals(userResource.getFiscalCode(), actualUser.getTaxCode());

        UserProductResponse actualUserWithoutMail = actual.get(1);
        assertNull(actualUserWithoutMail.getEmail());
    }

    UserResource dummyUserResource() {
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        userResource.setFiscalCode("test");
        userResource.setBirthDate(BirthDateCertifiableSchema.builder().value(LocalDate.now()).build());
        userResource.setEmail(EmailCertifiableSchema.builder().value("test@test.com").build());
        userResource.setName(NameCertifiableSchema.builder().value("testName").build());
        userResource.setFamilyName(FamilyNameCertifiableSchema.builder().value("testFamilyName").build());
        return userResource;
    }

    @Test
    void testRetrievePerson() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId("test-user");
        String userMailUuId = UUID.randomUUID().toString();
        userInstitution.setUserMailUuid(userMailUuId);

        UserResource userResource = dummyUserResource();
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(EmailCertifiableSchema.builder().value("userMail").build());
        userResource.setWorkContacts(Map.of(userMailUuId, workContactResource));

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<UserResponse> subscriber = userService.retrievePerson("test-user", "test-product", "test-institutionId").subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }

    @Test
    void testRetrievePerson_workContractsIsEmpty() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId("test-user");
        String userMailUuId = UUID.randomUUID().toString();
        userInstitution.setUserMailUuid(userMailUuId);

        UserResource userResource = dummyUserResource();
        WorkContactResource workContactResource = new WorkContactResource();
        userResource.setWorkContacts(Map.of(userMailUuId, workContactResource));

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<UserResponse> subscriber = userService.retrievePerson("test-user", "test-product", "test-institutionId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        UserResponse actual = subscriber.assertCompleted().getItem();
        assertNull(actual.getEmail());
        assertEquals(userResource.getName().getValue(), actual.getName());
    }

    @Test
    void testRetrievePersonFailsWhenUserIsNotPresent() {
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().nullItem());

        UniAssertSubscriber<UserResponse> subscriber = userService
                .retrievePerson("test-user", "test-product", "test-institutionId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);
    }

    @Test
    void testRetrievePersonFailsWhenPdvFails() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId("test-user");
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().failure(new ClientWebApplicationException(HttpStatus.SC_NOT_FOUND)));

        UniAssertSubscriber<UserResponse> subscriber = userService.retrievePerson("test-user", "test-product", "test-institutionId").subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);
    }

    @Test
    void testRetrieveBindingsOk() {
        UserInfo userInfoResponse = new UserInfo();
        userInfoResponse.setUserId("test-user");

        UserInstitutionRole userInstitution = new UserInstitutionRole();
        userInstitution.setInstitutionName("test-institutionId");
        userInstitution.setStatus(OnboardedProductState.ACTIVE);

        List<UserInstitutionRole> userInstitutionRoleResponses = new ArrayList<>();
        userInstitutionRoleResponses.add(userInstitution);
        userInfoResponse.setInstitutions(userInstitutionRoleResponses);

        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(userInfoResponse)));
        when(userUtils.filterInstitutionRoles(any(), any(), any()))
                .thenReturn(userInfoResponse);

        UniAssertSubscriber<UserInfo> subscriber = userService
                .retrieveBindings(null, "test-user", null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(userInfoResponse);
    }

    @Test
    void searchUserByFiscalCode() {
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().item(createUserInstitution()));

        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        final String institutionId = "institutionId";
        UniAssertSubscriber<UserDetailResponse> subscriber = userService
                .searchUserByFiscalCode("userId", institutionId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();

    }

    @Test
    void searchUserByFiscalCode_notFound() {
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().nullItem());

        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        final String institutionId = "institutionId";
        UniAssertSubscriber<UserDetailResponse> subscriber = userService
                .searchUserByFiscalCode("userId", institutionId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();

    }

    @Test
    void testRetrieveBindingsFails() {
        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        UniAssertSubscriber<UserInfo> subscriber = userService
                .retrieveBindings("test-institutionId", "test-user", null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);

    }

    @Test
    void testRetrieveBindingsFails2() {
        UserInstitutionRole userInstitution = new UserInstitutionRole();
        userInstitution.setInstitutionName("test-institutionId");
        userInstitution.setStatus(OnboardedProductState.PENDING);

        List<UserInstitutionRole> userInstitutionRoleResponses = new ArrayList<>();
        userInstitutionRoleResponses.add(userInstitution);

        UserInfo userInfoResponse = new UserInfo();
        userInfoResponse.setUserId("test-user");
        userInfoResponse.setInstitutions(userInstitutionRoleResponses);

        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(userInfoResponse)));
        when(userUtils.filterInstitutionRoles(any(), any(), any()))
                .thenReturn(new UserInfo());
        String[] states = {"ACTIVE"};

        UniAssertSubscriber<UserInfo> subscriber = userService
                .retrieveBindings("test-institutionId", "test-user", states)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);

    }

    @Test
    void updateUserStatusWithOptionalFilter() {
        when(userInstitutionService
                .updateUserStatusWithOptionalFilterByInstitutionAndProduct("userId", "institutionId", "prod-pagopa", MANAGER, null, OnboardedProductState.ACTIVE)).thenReturn(Uni.createFrom().item(1L));

        UniAssertSubscriber<Void> subscriber = userService
                .updateUserStatusWithOptionalFilter("userId", "institutionId", "prod-pagopa", MANAGER, null, OnboardedProductState.ACTIVE)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
    }

    @Test
    void updateUserStatusWithOptionalFilterUserNotFound() {
        when(userInstitutionService
                .updateUserStatusWithOptionalFilterByInstitutionAndProduct("userId", "institutionId", "prod-pagopa", MANAGER, null, OnboardedProductState.ACTIVE)).thenReturn(Uni.createFrom().item(0L));

        UniAssertSubscriber<Void> subscriber = userService
                .updateUserStatusWithOptionalFilter("userId", "institutionId", "prod-pagopa", MANAGER, null, OnboardedProductState.ACTIVE)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class, USER_TO_UPDATE_NOT_FOUND.getMessage());
    }

    @Test
    void updateUserStatusWithOptionalFilterInvalidRequest() {
        UniAssertSubscriber<Void> subscriber = userService
                .updateUserStatusWithOptionalFilter("userId", "institutionId", null, MANAGER, null, null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class, STATUS_IS_MANDATORY.getMessage());
    }

    @Test
    void retrieveUsersTest() {
        UserInstitution userInstitution = createUserInstitution();
        when(userInstitutionService.findAllWithFilter(any())).thenReturn(Multi.createFrom().item(userInstitution));
        AssertSubscriber<UserInstitutionResponse> subscriber = userService
                .findAllUserInstitutions("institutionId", "userId", null, null, null, null)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        List<UserInstitutionResponse> actual = subscriber.assertCompleted().getItems();
        Assert.assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(userInstitution.getUserId(), actual.get(0).getUserId());
    }

    @Test
    void findAllUserInstitutionsPaged_whenFilterProductIdAndProductRole() {
        UserInstitution userInstitution = createUserInstitution();
        final String productId = userInstitution.getProducts().get(0).getProductId();
        final String productRole = userInstitution.getProducts().get(0).getProductRole();
        when(userInstitutionService.paginatedFindAllWithFilter(anyMap(), anyInt(), anyInt())).thenReturn(Multi.createFrom().item(userInstitution));
        AssertSubscriber<UserInstitutionResponse> subscriber = userService
                .findPaginatedUserInstitutions("institutionId", "userId", null, null, List.of(productId), List.of(productRole), 0, 100)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        List<UserInstitutionResponse> actual = subscriber.assertCompleted().getItems();
        Assert.assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(userInstitution.getUserId(), actual.get(0).getUserId());
        assertEquals(productId, actual.get(0).getProducts().get(0).getProductId());
    }

    @Test
    void findAllUserInstitutionsPaged_whenFilterStatus() {
        UserInstitution userInstitution = createUserInstitution();
        when(userInstitutionService.paginatedFindAllWithFilter(anyMap(), anyInt(), anyInt())).thenReturn(Multi.createFrom().item(userInstitution));
        AssertSubscriber<UserInstitutionResponse> subscriber = userService
                .findPaginatedUserInstitutions("institutionId", "userId", null, List.of(OnboardedProductState.ACTIVE.name()), null, null, 0, 100)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        List<UserInstitutionResponse> actual = subscriber.assertCompleted().getItems();
        Assert.assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(userInstitution.getUserId(), actual.get(0).getUserId());
        assertEquals(OnboardedProductState.ACTIVE, actual.get(0).getProducts().get(0).getStatus());
    }

    @Test
    void findAllUserInstitutionsPaged_whenFilterRole() {
        UserInstitution userInstitution = createUserInstitution();
        when(userInstitutionService.paginatedFindAllWithFilter(anyMap(), anyInt(), anyInt())).thenReturn(Multi.createFrom().item(userInstitution));
        AssertSubscriber<UserInstitutionResponse> subscriber = userService
                .findPaginatedUserInstitutions("institutionId", "userId", List.of(MANAGER), null, null, null, 0, 100)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        List<UserInstitutionResponse> actual = subscriber.assertCompleted().getItems();
        Assert.assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(userInstitution.getUserId(), actual.get(0).getUserId());
        assertEquals(MANAGER.name(), actual.get(0).getProducts().get(0).getRole());
    }

    @Test
    void deleteUserInstitutionProductFound() {
        when(userInstitutionService.deleteUserInstitutionProduct("userId", "institutionId", "productId")).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Void> subscriber = userService
                .deleteUserInstitutionProduct("userId", "institutionId", "productId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();

    }

    @Test
    void deleteUserInstitutionProductNotFound() {
        when(userInstitutionService.deleteUserInstitutionProduct("userId", "institutionId", "productId")).thenReturn(Uni.createFrom().item(0L));
        UniAssertSubscriber<Void> subscriber = userService
                .deleteUserInstitutionProduct("userId", "institutionId", "productId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class, USER_TO_UPDATE_NOT_FOUND.getMessage());
    }

    @Test
    void deleteUserInstitutionProductUsers() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        when(userInstitutionService.deleteUserInstitutionProductUsers(institutionId, productId)).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<DeletedUserCountResponse> subscriber = userService
                .deleteUserInstitutionProductUsers(institutionId, productId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(new DeletedUserCountResponse(institutionId, productId, 1L));
    }

    @Test
    void deleteUserInstitutionProductUsersFail() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        when(userInstitutionService.deleteUserInstitutionProductUsers(institutionId, productId)).thenReturn(Uni.createFrom().failure(new RuntimeException()));
        UniAssertSubscriber<DeletedUserCountResponse> subscriber = userService
                .deleteUserInstitutionProductUsers(institutionId, productId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailedWith(RuntimeException.class);
    }

    @Test
    void findAllByIds() {
        //given
        List<String> userIds = List.of("userId");
        when(userInstitutionService.findAllWithFilter(any()))
                .thenReturn(Multi.createFrom().item(createUserInstitution()));
        //when
        UniAssertSubscriber<List<UserInstitutionResponse>> subscriber = userService.findAllByIds(userIds)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        //then
        List<UserInstitutionResponse> users = subscriber.assertCompleted().getItem();
        assertFalse(users.isEmpty());
        assertEquals(1, users.size());
    }

    @Test
    void updateUserCreatedAtUserNotFound() {
        final String productId = "productId";
        final String userId = "userId";
        final String institutionId = "institutionId";
        final OffsetDateTime now = OffsetDateTime.now();
        when(userInstitutionService
                .updateUserCreatedAtByInstitutionAndProduct(institutionId, List.of(userId), productId, now)).thenReturn(Uni.createFrom().item(0L));

        UniAssertSubscriber<Void> subscriber = userService
                .updateUserProductCreatedAt(institutionId, List.of(userId), productId, now)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class, USERS_TO_UPDATE_NOT_FOUND.getMessage());
    }

    @Test
    void updateUserProductCreatedAt() {
        when(userInstitutionService
                .updateUserCreatedAtByInstitutionAndProduct(anyString(), any(), anyString(), any()))
                .thenReturn(Uni.createFrom().item(1L));

        UniAssertSubscriber<Void> subscriber = userService
                .updateUserProductCreatedAt("institutionId", List.of("userId"), "productId", OffsetDateTime.now())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }

    @Test
    void findPaginatedUserNotificationToSend() {
        when(userInstitutionService.paginatedFindAllWithFilter(any(), any(), any()))
                .thenReturn(Multi.createFrom().item(createUserInstitution()));
        UserResource userResource = mock(UserResource.class);
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<List<UserNotificationToSend>> subscriber = userService
                .findPaginatedUserNotificationToSend(10, 0, "productId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }

    @Test
    void findPaginatedUserNotificationToSendQueryWithoutProductId() {
        when(userInstitutionService.paginatedFindAllWithFilter(any(), any(), any()))
                .thenReturn(Multi.createFrom().item(createUserInstitution()));
        UserResource userResource = mock(UserResource.class);
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        userService
                .findPaginatedUserNotificationToSend(10, 0, null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
    }

    @Test
    void testUpdateUserStatus() {
        UserResource userResource = mock(UserResource.class);
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UserInstitution userInstitutionResponse = mock(UserInstitution.class);
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap()))
                .thenReturn(Uni.createFrom().item(userInstitutionResponse));

        Product product = mock(Product.class);
        when(productService.getProduct(any())).thenReturn(product);

        when(userInstitutionService
                .updateUserStatusWithOptionalFilterByInstitutionAndProduct(
                        "userId", "institutionId", "productId", null, "productRole", OnboardedProductState.ACTIVE))
                .thenReturn(Uni.createFrom().item(1L));


        when(userNotificationService.sendEmailNotification(
                any(UserResource.class),
                any(UserInstitution.class),
                any(Product.class),
                any(),
                any(),
                anyString(),
                anyString())
        ).thenReturn(Uni.createFrom().voidItem());

        var subscriber = userService.updateUserProductStatus("userId", "institutionId", "productId", OnboardedProductState.ACTIVE, "productRole",
                        LoggedUser.builder().build())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitItem();

        verify(userNotificationService, times(1)).sendEmailNotification(
                any(UserResource.class),
                any(UserInstitution.class),
                any(Product.class),
                any(OnboardedProductState.class),
                any(),
                eq(null),
                eq(null)
        );
    }

    @Test
    void updateUserProductStatusNotFound() {
        UserResource userResource = mock(UserResource.class);
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService
                .updateUserStatusWithOptionalFilterByInstitutionAndProduct(
                        "userId", "institutionId", "productId", null, null, OnboardedProductState.ACTIVE))
                .thenReturn(Uni.createFrom().item(1L));
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("not found")));

        var subscriber = userService.updateUserProductStatus("userId", "institutionId", "productId", OnboardedProductState.ACTIVE, "productRole",
                        LoggedUser.builder().build())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);

    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_SuccessByFiscalCode() {
        // Prepare test data
        final String fiscalCode = "fiscalCode";
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode(fiscalCode + " ");
        CreateUserDto.Product createUserProduct = new CreateUserDto.Product();
        createUserProduct.setProductId("prod-io");
        createUserProduct.setProductRoles(List.of("admin2"));
        createUserDto.setUser(user);
        createUserDto.setProduct(createUserProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userRegistryApi.updateUsingPATCH(any(), any())).thenReturn(Uni.createFrom().item(Response.ok().build()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));

        // Call the method
        UniAssertSubscriber<CreateOrUpdateUserByFiscalCodeResponse> subscriber = userService.createOrUpdateUserByFiscalCode(createUserDto, loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        CreateOrUpdateUserByFiscalCodeResponse response = subscriber.awaitItem().getItem();

        assertEquals(userId.toString(), response.getUserId());

        // Verify fiscalCode trim
        ArgumentCaptor<UserSearchDto> captorFiscalCode = ArgumentCaptor.forClass(UserSearchDto.class);
        verify(userRegistryApi).searchUsingPOST(any(), captorFiscalCode.capture());
        assertEquals(fiscalCode, captorFiscalCode.getValue().getFiscalCode());

        verify(userRegistryApi).updateUsingPATCH(any(), any());
        verify(userInstitutionService).persistOrUpdate(any());
        verify(userInstitutionService).findByUserIdAndInstitutionId(any(), any());
        verify(userNotificationService).sendCreateUserNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCreateOrUpdateUser_ByFiscalCode_Exception_differentRole() {
        // Arrange
        String fiscalCode = "fiscalCode";
        String institutionId = "inst-002";
        String productId = PROD_INTEROP.getValue();
        List<String> roles = List.of("admin");

        CreateUserDto userDto = getCreateUserDto(productId, fiscalCode, institutionId, roles, DELEGATE.name());

        LoggedUser loggedUser = LoggedUser.builder().build();

        UserResource user = new UserResource();
        user.setId(UUID.randomUUID());
        user.setWorkContacts(new HashMap<>());

        when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(user));

        when(userUtils.getMailUuidFromMail(any(), any()))
                .thenReturn(Optional.of("mail-uuid"));

        when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.ok().build()));

        OnboardedProduct onboarded1 = new OnboardedProduct();
        onboarded1.setProductId(productId);
        onboarded1.setProductRole("operator-api");
        onboarded1.setStatus(SUSPENDED);
        onboarded1.setRole(OPERATOR);

        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setProducts(new ArrayList<>(List.of(onboarded1)));

        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any()))
                .thenReturn(Uni.createFrom().item(userInstitution));

        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                userService.createOrUpdateUserByFiscalCode(userDto, loggedUser)
                        .await().indefinitely());

        assertEquals("User already has different role on Product prod-interop", exception.getMessage());

    }

    @Test
    void testCreateOrUpdateUser_ByFiscalCode_Exception_hasAlreadyRoles() {
        // Arrange
        String fiscalCode = "fiscalCode";
        String institutionId = "inst-002";
        String productId = PROD_INTEROP.getValue();
        List<String> requestedRoles = List.of("admin");

        CreateUserDto userDto = getCreateUserDto(productId, fiscalCode, institutionId, requestedRoles, DELEGATE.name());
        LoggedUser loggedUser = LoggedUser.builder().build();

        UserResource user = new UserResource();
        user.setId(UUID.randomUUID());
        user.setWorkContacts(new HashMap<>());

        when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(user));

        when(userUtils.getMailUuidFromMail(any(), any()))
                .thenReturn(Optional.of("mail-uuid"));

        when(userRegistryApi.updateUsingPATCH(any(), any()))
                .thenReturn(Uni.createFrom().item(Response.ok().build()));

        OnboardedProduct onboarded = new OnboardedProduct();
        onboarded.setProductId(productId);
        onboarded.setProductRole("admin");
        onboarded.setStatus(ACTIVE);
        onboarded.setRole(DELEGATE);

        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setProducts(List.of(onboarded));

        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any()))
                .thenReturn(Uni.createFrom().item(userInstitution));

        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                userService.createOrUpdateUserByFiscalCode(userDto, loggedUser)
                        .await().indefinitely());

        assertEquals("User already has roles on Product prod-interop", exception.getMessage());
    }

    @Test
    void testSendMail() {
        UserResource user = mock(UserResource.class);
        when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, "userId"))
                .thenReturn(Uni.createFrom().item(user));

        UserResource loggedUser = new UserResource();
        loggedUser.setName(new NameCertifiableSchema(NameCertifiableSchema.CertificationEnum.SPID,"name"));
        loggedUser.setFamilyName(new FamilyNameCertifiableSchema(FamilyNameCertifiableSchema.CertificationEnum.SPID, "familyName"));
        when(userRegistryApi.findByIdUsingGET(USER_FIELD_LIST_WITHOUT_WORK_CONTACTS, "loggedUserId"))
                .thenReturn(Uni.createFrom().item(loggedUser));

        Product product = mock(Product.class);
        when(productService.getProduct(any())).thenReturn(product);

        when(userNotificationService.buildDataModelRequestAndSendEmail(
                any(UserResource.class),
                any(UserInstitution.class),
                any(Product.class),
                any(PartyRole.class),
                anyString(),
                anyString())
        ).thenReturn(Uni.createFrom().voidItem());

        var subscriber = userService.sendMailUserRequest("userId", "userMailUuid", "institutionId", "productId", DELEGATE, "loggedUserId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitItem();

        verify(userNotificationService, times(1)).buildDataModelRequestAndSendEmail(
                any(UserResource.class),
                any(UserInstitution.class),
                any(Product.class),
                any(PartyRole.class),
                anyString(),
                anyString()
        );
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_SuccessByFiscalCode_with2role() {
        UserInstitution userInstitution = createUserInstitution();
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        CreateUserDto.Product createUserProduct = new CreateUserDto.Product();
        createUserProduct.setProductId("test");
        createUserProduct.setRole(MANAGER.name());
        createUserProduct.setProductRoles(List.of("admin2", "admin3"));
        createUserDto.setUser(user);
        createUserDto.setProduct(createUserProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(any(), any())).thenReturn(Uni.createFrom().item(Response.ok().build()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));

        // Call the method
        UniAssertSubscriber<CreateOrUpdateUserByFiscalCodeResponse> subscriber = userService.createOrUpdateUserByFiscalCode(createUserDto, loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Assertions.assertEquals(4, userInstitution.getProducts().size());
        // Verify the result

        CreateOrUpdateUserByFiscalCodeResponse response = subscriber.awaitItem().getItem();

        assertEquals(userId.toString(), response.getUserId());
        verify(userRegistryApi).updateUsingPATCH(any(), any());
        verify(userInstitutionService).persistOrUpdate(any());
        verify(userInstitutionService).findByUserIdAndInstitutionId(any(), any());
        verify(userNotificationService).sendCreateUserNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_SuccessByFiscalCode_with2role_oneAlreadyOnboarded() {
        UserInstitution userInstitution = createUserInstitution();
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        CreateUserDto.Product createUserProduct = new CreateUserDto.Product();
        createUserProduct.setProductId("test");
        createUserProduct.setRole(MANAGER.name());
        createUserProduct.setProductRoles(List.of("admin", "admin3"));
        createUserDto.setUser(user);
        createUserDto.setProduct(createUserProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(any(), any())).thenReturn(Uni.createFrom().item(Response.ok().build()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));

        // Call the method
        UniAssertSubscriber<CreateOrUpdateUserByFiscalCodeResponse> subscriber = userService.createOrUpdateUserByFiscalCode(createUserDto, loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Assertions.assertEquals(3, userInstitution.getProducts().size());
        // Verify the result

        CreateOrUpdateUserByFiscalCodeResponse response = subscriber.awaitItem().getItem();

        assertEquals(userId.toString(), response.getUserId());
        verify(userRegistryApi).updateUsingPATCH(any(), any());
        verify(userInstitutionService).persistOrUpdate(any());
        verify(userInstitutionService).findByUserIdAndInstitutionId(any(), any());
        verify(userNotificationService).sendCreateUserNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_SuccessByFiscalCode_with2role_oneAlreadyOnboarded_withDifferentSelcRole() {
        UserInstitution userInstitution = createUserInstitution();
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        CreateUserDto.Product createUserProduct = new CreateUserDto.Product();
        createUserProduct.setProductId("test");
        createUserProduct.setRole(PartyRole.OPERATOR.name());
        createUserProduct.setProductRoles(List.of("admin", "admin3"));
        createUserDto.setUser(user);
        createUserDto.setProduct(createUserProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(any(), any())).thenReturn(Uni.createFrom().item(Response.ok().build()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));

        userService.createOrUpdateUserByFiscalCode(createUserDto, loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(InvalidRequestException.class, "User already has different role on Product test");

        Assertions.assertEquals(2, userInstitution.getProducts().size());
    }


    @Test
    void testCreateOrUpdateUser_CreateUser_SuccessByFiscalCode() {
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        createUserDto.setUser(user);
        CreateUserDto.Product createUserProduct = new CreateUserDto.Product();
        createUserProduct.setProductId("productId");
        createUserProduct.setProductRoles(List.of("admin2"));
        createUserDto.setProduct(createUserProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().failure(new WebClientApplicationException(HttpStatus.SC_NOT_FOUND)));
        when(userRegistryApi.saveUsingPATCH(any())).thenReturn(Uni.createFrom().item(org.openapi.quarkus.user_registry_json.model.UserId.builder().id(UUID.fromString(userId.toString())).build()));
        when(userInstitutionService.persistOrUpdate(any())).thenAnswer(awr -> {
            UserInstitution saved = (UserInstitution) awr.getArguments()[0];
            saved.setId(ObjectId.get());
            return Uni.createFrom().item(saved);
        });
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));

        // Call the method
        UniAssertSubscriber<CreateOrUpdateUserByFiscalCodeResponse> subscriber = userService.createOrUpdateUserByFiscalCode(createUserDto, loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result

        CreateOrUpdateUserByFiscalCodeResponse response = subscriber.awaitItem().getItem();

        assertEquals(userId.toString(), response.getUserId());
        verify(userRegistryApi).saveUsingPATCH(any());
        verify(userRegistryApi).findByIdUsingGET(any(), eq(userId.toString()));
        verify(userInstitutionService).persistOrUpdate(any());
        verify(userNotificationService).sendCreateUserNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_UserRegistryUpdateFailedByFiscalCode() {
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        createUserDto.setUser(user);
        LoggedUser loggedUser = LoggedUser.builder().build();

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userRegistryApi.updateUsingPATCH(any(), any())).thenReturn(Uni.createFrom().failure(new RuntimeException()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(createUserInstitution()));

        // Call the method
        UniAssertSubscriber<CreateOrUpdateUserByFiscalCodeResponse> subscriber = userService.createOrUpdateUserByFiscalCode(createUserDto, loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.assertFailedWith(RuntimeException.class);
        verify(userRegistryApi).updateUsingPATCH(any(), any());
        verify(userInstitutionService, never()).persistOrUpdate(any());
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_UserInstitutionUpdateFailedByFiscalCode() {
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        CreateUserDto.Product createUserProduct = new CreateUserDto.Product();
        createUserProduct.setProductId("productId");
        createUserProduct.setProductRoles(List.of("admin2"));
        createUserDto.setProduct(createUserProduct);
        createUserDto.setUser(user);
        LoggedUser loggedUser = LoggedUser.builder().build();

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userRegistryApi.updateUsingPATCH(any(), any())).thenReturn(Uni.createFrom().item(Response.ok().build()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().failure(new RuntimeException()));

        // Call the method
        UniAssertSubscriber<CreateOrUpdateUserByFiscalCodeResponse> subscriber = userService.createOrUpdateUserByFiscalCode(createUserDto, loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.assertFailedWith(RuntimeException.class);
        verify(userRegistryApi).updateUsingPATCH(any(), any());
        verify(userInstitutionService).persistOrUpdate(any());
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_ProductAlreadyOnboarded() {
        // Prepare test data
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setDelegationId("delegationId");
        addUserRoleProduct.setRole(MANAGER.name());
        addUserRoleProduct.setProductRoles(List.of("admin"));
        addUserRoleDto.setProduct(addUserRoleProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        when(userRegistryApi.findByIdUsingGET(any(), eq("userId"))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), addUserRoleDto.getInstitutionId())).thenReturn(Uni.createFrom().item(createUserInstitution()));

        userService.createOrUpdateUserByUserId(addUserRoleDto, "userId", loggedUser, ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(InvalidRequestException.class, "User already has roles on Product test");
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_ProductAlreadyOnboardedWithAnotherRole() {
        // Prepare test data
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(OPERATOR.name());
        addUserRoleProduct.setDelegationId("delegationId");
        addUserRoleDto.setProduct(addUserRoleProduct);
        addUserRoleProduct.setProductRoles(List.of("operatore api"));
        LoggedUser loggedUser = LoggedUser.builder().build();

        when(userRegistryApi.findByIdUsingGET(any(), eq("userId"))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), addUserRoleDto.getInstitutionId())).thenReturn(Uni.createFrom().item(createUserInstitution()));


        userService.createOrUpdateUserByUserId(addUserRoleDto, "userId", loggedUser, ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(InvalidRequestException.class, "User already has different role on Product test");

    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_ProductAlreadyOnboardedWithAnotherProductRole() {
        // Prepare test data
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(OPERATOR.name());
        addUserRoleProduct.setDelegationId("delegationId");
        addUserRoleDto.setProduct(addUserRoleProduct);
        addUserRoleProduct.setProductRoles(List.of("security"));
        LoggedUser loggedUser = LoggedUser.builder().build();
        UserInstitution userInstitution = createUserInstitution();
        userInstitution.getProducts().get(0).setStatus(DELETED);
        userInstitution.getProducts().get(1).setStatus(ACTIVE);
        userInstitution.getProducts().get(1).setProductId("test");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        Product product = new Product();
        product.setDescription("description");

        // Mock external dependencies
        when(userRegistryApi.findByIdUsingGET(any(), eq("userId"))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), addUserRoleDto.getInstitutionId())).thenReturn(Uni.createFrom().item((userInstitution)));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));
        when(userNotificationService.sendKafkaNotification(any())).thenReturn(Uni.createFrom().item(userNotificationToSend));


        // Call the method
        UniAssertSubscriber<String> subscriber = userService.createOrUpdateUserByUserId(addUserRoleDto, "userId", loggedUser, ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.awaitItem().assertCompleted();
        verify(userRegistryApi).findByIdUsingGET(any(), eq("userId"));
        verify(userInstitutionService).persistOrUpdate(any());

    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_OneOfProductRoleAlreadyOnboarded() {
        // Prepare test data
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(MANAGER.name());
        addUserRoleDto.setProduct(addUserRoleProduct);
        addUserRoleProduct.setProductRoles(List.of("admin", "admin2"));
        LoggedUser loggedUser = LoggedUser.builder().build();

        Product product = new Product();
        product.setDescription("description");

        when(userRegistryApi.findByIdUsingGET(any(), eq("userId"))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), addUserRoleDto.getInstitutionId())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(productService.getProduct(any())).thenReturn(product);

        // Call the method
        UniAssertSubscriber<String> subscriber = userService.createOrUpdateUserByUserId(addUserRoleDto, "userId", loggedUser, ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        assertNull(subscriber.awaitItem().getItem());
        verify(userRegistryApi).findByIdUsingGET(any(), eq("userId"));
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_SuccessByUserId() {
        // Prepare test data
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("prod-io-premium");
        addUserRoleProduct.setProductRoles(List.of("admin2"));
        addUserRoleProduct.setDelegationId("delegationId");
        addUserRoleDto.setProduct(addUserRoleProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        // Mock external dependencies
        when(userRegistryApi.findByIdUsingGET(any(), eq("userId"))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), addUserRoleDto.getInstitutionId())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));
        when(userNotificationService.sendKafkaNotification(any())).thenReturn(Uni.createFrom().item(userNotificationToSend));


        // Call the method
        UniAssertSubscriber<String> subscriber = userService.createOrUpdateUserByUserId(addUserRoleDto, "userId", loggedUser, ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.awaitItem().assertCompleted();
        verify(userRegistryApi).findByIdUsingGET(any(), eq("userId"));
        verify(userInstitutionService).persistOrUpdate(any());
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_SuccessByUserId2() {
        // Prepare test data
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("productId");
        addUserRoleDto.setProduct(addUserRoleProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        // Mock external dependencies
        when(userRegistryApi.findByIdUsingGET(any(), eq("userId"))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), addUserRoleDto.getInstitutionId())).thenReturn(Uni.createFrom().nullItem());
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));
        when(userNotificationService.sendKafkaNotification(any())).thenReturn(Uni.createFrom().item(userNotificationToSend));


        // Call the method
        UniAssertSubscriber<String> subscriber = userService.createOrUpdateUserByUserId(addUserRoleDto, "userId", loggedUser, ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.awaitItem().assertCompleted();
        verify(userRegistryApi).findByIdUsingGET(any(), eq("userId"));
        verify(userInstitutionService).persistOrUpdate(any());
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_UserInstitutionUpdateFailedByUserId_userInstitutionNotFound() {
        // Prepare test data
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        LoggedUser loggedUser = LoggedUser.builder().build();

        // Mock external dependencies
        when(userRegistryApi.findByIdUsingGET(any(), eq("userId"))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(userResource.getId().toString(), addUserRoleDto.getInstitutionId())).thenReturn(Uni.createFrom().nullItem());
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().failure(new RuntimeException()));

        // Call the method
        UniAssertSubscriber<String> subscriber = userService.createOrUpdateUserByUserId(addUserRoleDto, "userId", loggedUser, ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.assertFailedWith(RuntimeException.class);
        verify(userRegistryApi).findByIdUsingGET(any(), eq("userId"));
    }

    @ParameterizedTest
    @EnumSource(value = OnboardedProductState.class, names = {"ACTIVE", "SUSPENDED"})
    void createUserFromOnboardingByUserIdUserIsAlreadyManager(OnboardedProductState status) {
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(MANAGER.name());
        addUserRoleDto.setProduct(addUserRoleProduct);

        LoggedUser loggedUser = LoggedUser.builder().build();
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        product.setProductRole("admin");
        product.setRole(MANAGER);
        product.setStatus(status);

        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setStatus(status);
        List<OnboardedProduct> products = new ArrayList<>();
        products.add(product);
        products.add(onboardedProduct);
        userInstitution.setProducts(products);

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap()))
                .thenReturn(Uni.createFrom().item(userInstitution));

        userService.createUserByUserId(addUserRoleDto, "userId", loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(UserRoleAlreadyPresentException.class, String.format("User already has MANAGER role with status %s for product [test].", status));
    }

    @Test
    void createUserFromOnboardingByUserIdInvalidRole() {
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole("INVALID");
        addUserRoleDto.setProduct(addUserRoleProduct);

        LoggedUser loggedUser = LoggedUser.builder().build();
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        product.setProductRole("admin");
        product.setRole(MANAGER);
        product.setStatus(OnboardedProductState.ACTIVE);

        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
        List<OnboardedProduct> products = new ArrayList<>();
        products.add(product);
        products.add(onboardedProduct);
        userInstitution.setProducts(products);

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap()))
                .thenReturn(Uni.createFrom().item(userInstitution));

        userService.createUserByUserId(addUserRoleDto, "userId", loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(InvalidRequestException.class, "Invalid role: INVALID. Allowed value are: [MANAGER, DELEGATE, SUB_DELEGATE, OPERATOR, ADMIN_EA]");
    }


    @ParameterizedTest
    @EnumSource(value = PartyRole.class, names = {"SUB_DELEGATE", "DELEGATE", "OPERATOR", "ADMIN_EA"})
    void createUserFromOnboardingByUserIdWhenIsAlreadyOnboarded(PartyRole role) {
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(role.name());
        addUserRoleDto.setProduct(addUserRoleProduct);

        LoggedUser loggedUser = LoggedUser.builder().build();
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        product.setProductRole("admin");
        product.setRole(MANAGER);
        product.setStatus(OnboardedProductState.ACTIVE);

        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
        List<OnboardedProduct> products = new ArrayList<>();
        products.add(product);
        products.add(onboardedProduct);
        userInstitution.setProducts(products);

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap()))
                .thenReturn(Uni.createFrom().item(userInstitution));

        userService.createUserByUserId(addUserRoleDto, "userId", loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(UserRoleAlreadyPresentException.class, String.format("User already has status ACTIVE for product [test]. Cannot assign %s role.", role));
    }

    @Test
    void createManagerByUserIdUserWhenIsAlreadyOnboardedWithDifferentRole() {
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(MANAGER.name());
        addUserRoleProduct.setProductRoles(List.of("admin2"));
        addUserRoleProduct.setDelegationId("delegationId");
        addUserRoleDto.setProduct(addUserRoleProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        UserInstitution userInstitution = createUserInstitutionWithoutManagerRole();

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), eq("userId"))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct("userId", addUserRoleDto.getInstitutionId(),
                addUserRoleProduct.getProductId(), null, null, DELETED)).thenReturn(Uni.createFrom().item(1L));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(createUserInstitutionWithoutManagerRole()));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));
        when(userNotificationService.sendKafkaNotification(any())).thenReturn(Uni.createFrom().item(userNotificationToSend));

        UniAssertSubscriber<String> subscriber = userService.createUserByUserId(addUserRoleDto, "userId", loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitItem().assertCompleted();
        verify(userRegistryApi).findByIdUsingGET(any(), eq("userId"));
        verify(userInstitutionService).persistOrUpdate(any());
    }

    @Test
    void createManagerByUserIdUserWithoutRoleOnProduct() {
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(MANAGER.name());
        addUserRoleProduct.setProductRoles(List.of("admin2"));
        addUserRoleProduct.setDelegationId("delegationId");
        addUserRoleDto.setProduct(addUserRoleProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        UserInstitution userInstitution = createUserInstitutionWithoutManagerRole();
        userInstitution.getProducts().forEach(onboardedProduct -> onboardedProduct.setRole(null));

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), eq("userId"))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct("userId", addUserRoleDto.getInstitutionId(),
                addUserRoleProduct.getProductId(), null, null, DELETED)).thenReturn(Uni.createFrom().item(1L));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(createUserInstitutionWithoutManagerRole()));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));
        when(userNotificationService.sendKafkaNotification(any())).thenReturn(Uni.createFrom().item(userNotificationToSend));

        UniAssertSubscriber<String> subscriber = userService.createUserByUserId(addUserRoleDto, "userId", loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitItem().assertCompleted();
        verify(userRegistryApi).findByIdUsingGET(any(), eq("userId"));
        verify(userInstitutionService).persistOrUpdate(any());
    }

    @Test
    void createManagerByUserIdUserInstitutionNotFound() {
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(MANAGER.name());
        addUserRoleDto.setProduct(addUserRoleProduct);
        LoggedUser loggedUser = LoggedUser.builder().build();

        Product product = new Product();
        product.setDescription("description");

        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId.toString());

        UserNotificationToSend userNotificationToSend = new UserNotificationToSend();
        userNotificationToSend.setUser(userToNotify);


        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap()))
                .thenReturn(Uni.createFrom().nullItem());
        when(userRegistryApi.findByIdUsingGET(any(), eq(userResource.getId().toString()))).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(userResource.getId().toString(), addUserRoleDto.getInstitutionId(),
                addUserRoleProduct.getProductId(), null, null, DELETED)).thenReturn(Uni.createFrom().item(1L));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().item(createUserInstitutionWithoutManagerRole()));
        when(productService.getProduct(any())).thenReturn(product);
        when(userNotificationService.sendCreateUserNotification(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(userUtils.buildUsersNotificationResponse(any(), any())).thenReturn(List.of(userNotificationToSend));
        when(userNotificationService.sendKafkaNotification(any())).thenReturn(Uni.createFrom().item(userNotificationToSend));


        userService.createUserByUserId(addUserRoleDto, userResource.getId().toString(), loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertItem(userResource.getId().toString())
                .assertCompleted();
    }

    @Test
    void createUserByUserIdWithUpdatedMail() {
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        addUserRoleDto.setUserMailUuid("newUserMailUuid");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(MANAGER.name());
        addUserRoleDto.setProduct(addUserRoleProduct);

        LoggedUser loggedUser = LoggedUser.builder().build();
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        product.setProductRole("admin");
        product.setRole(MANAGER);
        product.setStatus(OnboardedProductState.ACTIVE);

        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
        List<OnboardedProduct> products = new ArrayList<>();
        products.add(product);
        products.add(onboardedProduct);
        userInstitution.setProducts(products);
        userInstitution.setUserMailUuid("oldUserMailUuid");

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap()))
                .thenReturn(Uni.createFrom().item(userInstitution));
        when(userInstitutionService.persistOrUpdate(userInstitution))
                .thenReturn(Uni.createFrom().item(userInstitution));

        userService.createUserByUserId(addUserRoleDto, "userId", loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(UserRoleAlreadyPresentException.class);
        assertEquals("newUserMailUuid", userInstitution.getUserMailUuid());
        assertNotNull(userInstitution.getUserMailUpdatedAt());
    }

    @Test
    void createUserByUserIdWithoutUpdatedMail() {
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        addUserRoleDto.setUserMailUuid("userMailUuid");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(MANAGER.name());
        addUserRoleDto.setProduct(addUserRoleProduct);

        LoggedUser loggedUser = LoggedUser.builder().build();
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        product.setProductRole("admin");
        product.setRole(MANAGER);
        product.setStatus(OnboardedProductState.ACTIVE);

        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
        List<OnboardedProduct> products = new ArrayList<>();
        products.add(product);
        products.add(onboardedProduct);
        userInstitution.setProducts(products);
        userInstitution.setUserMailUuid("userMailUuid");

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap()))
                .thenReturn(Uni.createFrom().item(userInstitution));
        when(userInstitutionService.persistOrUpdate(userInstitution))
                .thenReturn(Uni.createFrom().item(userInstitution));

        userService.createUserByUserId(addUserRoleDto, "userId", loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(UserRoleAlreadyPresentException.class);
        assertEquals("userMailUuid", userInstitution.getUserMailUuid());
        assertNull(userInstitution.getUserMailUpdatedAt());
    }

    @Test
    void createUserByUserIdWithNullUserMailFromRequest() {
        AddUserRoleDto addUserRoleDto = new AddUserRoleDto();
        addUserRoleDto.setInstitutionId("institutionId");
        AddUserRoleDto.Product addUserRoleProduct = new AddUserRoleDto.Product();
        addUserRoleProduct.setProductId("test");
        addUserRoleProduct.setRole(MANAGER.name());
        addUserRoleDto.setProduct(addUserRoleProduct);

        LoggedUser loggedUser = LoggedUser.builder().build();
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        product.setProductRole("admin");
        product.setRole(MANAGER);
        product.setStatus(OnboardedProductState.ACTIVE);

        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
        List<OnboardedProduct> products = new ArrayList<>();
        products.add(product);
        products.add(onboardedProduct);
        userInstitution.setProducts(products);
        userInstitution.setUserMailUuid("userMailUuid");

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap()))
                .thenReturn(Uni.createFrom().item(userInstitution));
        when(userInstitutionService.persistOrUpdate(userInstitution))
                .thenReturn(Uni.createFrom().item(userInstitution));

        userService.createUserByUserId(addUserRoleDto, "userId", loggedUser)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(UserRoleAlreadyPresentException.class);
        assertEquals("userMailUuid", userInstitution.getUserMailUuid());
        assertNull(userInstitution.getUserMailUpdatedAt());
    }

    private UserInstitution createUserInstitutionWithoutManagerRole() {
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId("test");
        onboardedProduct.setRole(OPERATOR);
        onboardedProduct.setStatus(ACTIVE);
        userInstitution.setProducts(List.of(onboardedProduct));
        return userInstitution;
    }

    @Test
    void testRetrieveUsersData() {
        // Prepare test data
        String institutionId = "test-institution";
        String personId = "test-person";
        List<String> roles = Collections.singletonList("test-role");
        List<String> states = Collections.singletonList("test-state");
        List<String> products = Collections.singletonList("test-product");
        List<String> productRoles = Collections.singletonList("test-productRole");
        String userUuid = "test-userUuid";

        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId(userUuid);
        userInstitution.setInstitutionId(institutionId);

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        // Mock external dependencies
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        // Call the method
        AssertSubscriber<UserDataResponse> subscriber = userService.retrieveUsersData(institutionId, personId, roles, states, products, productRoles, userUuid)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        // Verify the result
        subscriber.assertCompleted().getItems().forEach(actual -> {
            assertNotNull(actual);
            assertEquals(institutionId, "test-institution");
        });

        // Verify the interactions
        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(anyMap());
        verify(userInstitutionService).findAllWithFilter(any());
        verify(userRegistryApi).findByIdUsingGET(any(), any());
    }

    @Test
    void testRetrieveUsersDataRemovingProductsWhenProductFilterIsPresent() {
        // Prepare test data
        String institutionId = "test-institution";
        String personId = "test-person";
        List<String> roles = Collections.emptyList();
        List<String> states = Collections.emptyList();
        List<String> products = Collections.singletonList("prod-io");
        List<String> productRoles = Collections.emptyList();
        String userUuid = "test-userUuid";

        UserInstitution userInstitution = new UserInstitution();

        OnboardedProduct prodIo = new OnboardedProduct();
        prodIo.setProductId("prod-io");
        prodIo.setStatus(OnboardedProductState.ACTIVE);
        prodIo.setRole(MANAGER);
        prodIo.setProductRole("test-productRole");
        OnboardedProduct prodPagoPa = new OnboardedProduct();
        prodPagoPa.setProductId("prod-pagopa");
        prodPagoPa.setStatus(OnboardedProductState.ACTIVE);
        prodPagoPa.setRole(MANAGER);
        prodPagoPa.setProductRole("test-productRole");
        userInstitution.setUserId(userUuid);
        userInstitution.setInstitutionId(institutionId);
        List<OnboardedProduct> productsList = new ArrayList<>();
        productsList.add(prodIo);
        productsList.add(prodPagoPa);
        userInstitution.setProducts(productsList);

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        // Mock external dependencies
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        // Call the method
        AssertSubscriber<UserDataResponse> subscriber = userService.retrieveUsersData(institutionId, personId, roles, states, products, productRoles, userUuid)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        // Verify the result
        subscriber.assertCompleted().getItems().forEach(actual -> {
            assertNotNull(actual);
            assertEquals(0, actual.getProducts().size());
            assertEquals(institutionId, "test-institution");
        });

        // Verify the interactions
        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(anyMap());
        verify(userInstitutionService).findAllWithFilter(any());
        verify(userRegistryApi).findByIdUsingGET(any(), any());
    }

    @Test
    void testRetrieveUsersDataRemovingProductsWhenStatesFilterIsPresent() {
        // Prepare test data
        String institutionId = "test-institution";
        String personId = "test-person";
        List<String> roles = Collections.emptyList();
        List<String> states = Collections.singletonList("ACTIVE");
        List<String> products = Collections.emptyList();
        List<String> productRoles = Collections.emptyList();
        String userUuid = "test-userUuid";

        UserInstitution userInstitution = new UserInstitution();

        OnboardedProduct prodIo = new OnboardedProduct();
        prodIo.setProductId("prod-io");
        prodIo.setStatus(OnboardedProductState.ACTIVE);
        prodIo.setRole(MANAGER);
        prodIo.setProductRole("test-productRole");
        OnboardedProduct prodPagoPa = new OnboardedProduct();
        prodPagoPa.setProductId("prod-pagopa");
        prodPagoPa.setStatus(OnboardedProductState.PENDING);
        prodPagoPa.setRole(MANAGER);
        prodPagoPa.setProductRole("test-productRole");
        userInstitution.setUserId(userUuid);
        userInstitution.setInstitutionId(institutionId);
        List<OnboardedProduct> productsList = new ArrayList<>();
        productsList.add(prodIo);
        productsList.add(prodPagoPa);
        userInstitution.setProducts(productsList);

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        // Mock external dependencies
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        // Call the method
        AssertSubscriber<UserDataResponse> subscriber = userService.retrieveUsersData(institutionId, personId, roles, states, products, productRoles, userUuid)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        // Verify the result
        subscriber.assertCompleted().getItems().forEach(actual -> {
            assertNotNull(actual);
            assertEquals(1, actual.getProducts().size());
            assertEquals("test", actual.getProducts().get(0).getProductId());
            assertEquals(institutionId, "test-institution");
        });

        // Verify the interactions
        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(anyMap());
        verify(userInstitutionService).findAllWithFilter(any());
        verify(userRegistryApi).findByIdUsingGET(any(), any());
    }

    @Test
    void testRetrieveUsersDataRemovingProductsWhenProductRolesFilterIsPresent() {
        // Prepare test data
        String institutionId = "test-institution";
        String personId = "test-person";
        List<String> roles = Collections.emptyList();
        List<String> states = Collections.emptyList();
        List<String> products = Collections.emptyList();
        List<String> productRoles = Collections.singletonList("admin");
        String userUuid = "test-userUuid";

        UserInstitution userInstitution = new UserInstitution();

        OnboardedProduct prodIo = new OnboardedProduct();
        prodIo.setProductId("prod-io");
        prodIo.setStatus(OnboardedProductState.ACTIVE);
        prodIo.setRole(MANAGER);
        prodIo.setProductRole("test-productRole");
        OnboardedProduct prodPagoPa = new OnboardedProduct();
        prodPagoPa.setProductId("prod-pagopa");
        prodPagoPa.setStatus(OnboardedProductState.ACTIVE);
        prodPagoPa.setRole(MANAGER);
        prodPagoPa.setProductRole("test-productRole2");
        userInstitution.setUserId(userUuid);
        userInstitution.setInstitutionId(institutionId);
        List<OnboardedProduct> productsList = new ArrayList<>();
        productsList.add(prodIo);
        productsList.add(prodPagoPa);
        userInstitution.setProducts(productsList);

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        // Mock external dependencies
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap())).thenReturn(Uni.createFrom().item(createUserInstitution()));
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        // Call the method
        AssertSubscriber<UserDataResponse> subscriber = userService.retrieveUsersData(institutionId, personId, roles, states, products, productRoles, userUuid)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        // Verify the result
        subscriber.assertCompleted().getItems().forEach(actual -> {
            assertNotNull(actual);
            assertEquals(1, actual.getProducts().size());
            assertEquals("test", actual.getProducts().get(0).getProductId());
            assertEquals(institutionId, "test-institution");
        });

        // Verify the interactions
        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(anyMap());
        verify(userInstitutionService).findAllWithFilter(any());
        verify(userRegistryApi).findByIdUsingGET(any(), any());
    }

    @Test
    void testRetrieveUsersDataWithNoAdminRole() {
        // Prepare test data
        String institutionId = "test-institution";
        String personId = "test-person";
        List<String> roles = Collections.singletonList("test-role");
        List<String> states = Collections.singletonList("test-state");
        List<String> products = Collections.singletonList("test-product");
        List<String> productRoles = Collections.singletonList("test-productRole");
        String userUuid = "test-userUuid";

        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId(userUuid);
        userInstitution.setInstitutionId(institutionId);

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        // Mock external dependencies
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap())).thenReturn(Uni.createFrom().nullItem());
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(createUserInstitution()));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        // Call the method
        AssertSubscriber<UserDataResponse> subscriber = userService.retrieveUsersData(institutionId, personId, roles, states, products, productRoles, userUuid)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        // Verify the result
        subscriber.assertCompleted().getItems().forEach(actual -> {
            assertNotNull(actual);
            assertEquals(institutionId, "test-institution");
        });

        // Verify the interactions
        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(anyMap());
        verify(userInstitutionService).findAllWithFilter(any());
        verify(userRegistryApi).findByIdUsingGET(any(), any());
    }

    @Test
    void updateInstitutionDescription() {
        // Prepare test data
        String institutionId = "institutionId";
        UpdateDescriptionDto descriptionDto = new UpdateDescriptionDto();
        descriptionDto.setInstitutionDescription("description");
        descriptionDto.setInstitutionRootName("rootName");

        // Mock external dependencies
        when(userInstitutionService.updateInstitutionDescription(any(), any())).thenReturn(Uni.createFrom().item(1L));

        // Call the method
        UniAssertSubscriber<Void> subscriber = userService
                .updateInstitutionDescription(institutionId, descriptionDto)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.assertCompleted();

        // Verify the interactions
        verify(userInstitutionService).updateInstitutionDescription(institutionId, descriptionDto);

    }

    @Test
    void testGetUserInstitutionWithPermissionQueryNoResult() {
        String institutionId = "institutionId";
        String userId = "userId";

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap())).thenReturn(Uni.createFrom().nullItem());
        userService.getUserInstitutionWithPermission(userId, institutionId, null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create()).assertFailedWith(ResourceNotFoundException.class);

        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(anyMap());

    }

    @Test
    void testGetUserInstitutionWithPermissionQueryWithoutProductId() {
        String institutionId = "institutionId";
        String userId = "userId";

        Map<String, Object> queryParameter;
        queryParameter = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter))
                .thenReturn(Uni.createFrom().item(createUserInstitution()));

        userService.getUserInstitutionWithPermission(userId, institutionId, null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertItem(getUserInstitutionWithAction())
                .assertCompleted();

        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(queryParameter);

    }

    @Test
    void testGetUserInstitutionWithPermissionQueryWithoutProductId_ADMIN_EA_IO() {
        String institutionId = "institutionId";
        String userId = "userId";

        Map<String, Object> queryParameter;
        queryParameter = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter))
                .thenReturn(Uni.createFrom().item(createUserInstitution_ADMIN_EA_IO()));

        userService.getUserInstitutionWithPermission(userId, institutionId, null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertItem(getUserInstitutionWithAction_ADMIN_EA_IO())
                .assertCompleted();

        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(queryParameter);

    }

    private UserInstitutionWithActions getUserInstitutionWithAction() {
        UserInstitutionWithActions userInstitutionWithActions = new UserInstitutionWithActions();
        OnboardedProductWithActions product = new OnboardedProductWithActions();
        product.setRole(MANAGER.name());
        product.setProductId("test");
        product.setProductRole("admin");
        product.setStatus(ACTIVE);
        product.setUserProductActions(List.of("Selc:UploadLogo",
                "Selc:ViewBilling",
                "Selc:RequestProductAccess",
                "Selc:ListAvailableProducts",
                "Selc:ListActiveProducts",
                "Selc:AccessProductBackoffice",
                "Selc:ViewManagedInstitutions",
                "Selc:ViewDelegations",
                "Selc:ManageProductUsers",
                "Selc:ListProductUsers",
                "Selc:ManageProductGroups",
                "Selc:CreateDelegation",
                "Selc:ViewInstitutionData"));
        userInstitutionWithActions.setInstitutionRootName("institutionRootName");
        userInstitutionWithActions.setUserMailUuid(workContractsKey);
        userInstitutionWithActions.setInstitutionId("institutionId");
        userInstitutionWithActions.setUserId(userId.toString());
        userInstitutionWithActions.setProducts(List.of(product));
        return userInstitutionWithActions;
    }

    private UserInstitutionWithActions getUserInstitutionWithAction_ADMIN_EA_IO() {
        UserInstitutionWithActions userInstitutionWithActions = new UserInstitutionWithActions();
        OnboardedProductWithActions product = new OnboardedProductWithActions();
        product.setRole(ADMIN_EA.name());
        product.setProductId("prod-io");
        product.setProductRole("admin");
        product.setStatus(ACTIVE);
        product.setUserProductActions(List.of("Selc:UploadLogo",
                "Selc:ViewBilling",
                "Selc:ListActiveProducts",
                "Selc:AccessProductBackoffice",
                "Selc:ViewManagedInstitutions",
                "Selc:ViewDelegations",
                "Selc:ManageProductGroups",
                "Selc:CreateDelegation",
                "Selc:ViewInstitutionData",
                "Selc:ViewContract"));
        userInstitutionWithActions.setInstitutionRootName("institutionRootName");
        userInstitutionWithActions.setUserMailUuid(workContractsKey);
        userInstitutionWithActions.setInstitutionId("institutionId");
        userInstitutionWithActions.setUserId(userId.toString());
        userInstitutionWithActions.setProducts(List.of(product));
        return userInstitutionWithActions;
    }

    @Test
    void testGetUserInstitutionWithPermissionQueryWithProductId() {
        String productId = "productId";
        String institutionId = "institutionId";
        String userId = "userId";

        Map<String, Object> queryParameter;
        Map<String, Object> userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        Map<String, Object> productFilters = OnboardedProductFilter.builder().productId(productId).status(ACTIVE).build().constructMap();
        queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter))
                .thenReturn(Uni.createFrom().item(createUserInstitution()));


        userService.getUserInstitutionWithPermission(userId, institutionId, productId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(queryParameter);

    }

    @Test
    void testGetUserInstitutionWithPermissionQueryWithProductWithParent() {
        String productId = "prod-io-premium";
        String institutionId = "institutionId";
        String userId = "userId";
        String parentProductId = "prod-io";

        Map<String, Object> queryParameter;
        Map<String, Object> userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        Map<String, Object> productFilters = OnboardedProductFilter.builder().productId(parentProductId).status(ACTIVE).build().constructMap();
        queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter))
                .thenReturn(Uni.createFrom().item(createUserInstitution()));


        userService.getUserInstitutionWithPermission(userId, institutionId, productId)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(queryParameter);

    }

    @Test
    void testGetUsersCount() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<PartyRole> roles = List.of(PartyRole.MANAGER, PartyRole.DELEGATE);
        final List<OnboardedProductState> status = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING);

        when(userInstitutionService.countUsers(institutionId, productId, roles, status))
                .thenReturn(Uni.createFrom().item(2L));

        userService.getUsersCount(institutionId, productId, roles, status).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertItem(new UsersCountResponse(institutionId, productId, roles, status, 2L))
                .assertCompleted();

        verify(userInstitutionService).countUsers(institutionId, productId, roles, status);
    }

    @Test
    void testGetUsersCountWithoutRolesAndStatus() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<PartyRole> fullListOfRoles = List.of(PartyRole.values());

        when(userInstitutionService.countUsers(institutionId, productId, fullListOfRoles, List.of(ACTIVE)))
                .thenReturn(Uni.createFrom().item(2L));

        userService.getUsersCount(institutionId, productId, null, null).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertItem(new UsersCountResponse(institutionId, productId, fullListOfRoles, List.of(ACTIVE), 2L))
                .assertCompleted();

        verify(userInstitutionService).countUsers(institutionId, productId, fullListOfRoles, List.of(ACTIVE));
    }

    @Test
    void testGetUsersCountWithEmptyRolesAndStatus() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<PartyRole> roles = new ArrayList<>();
        final List<OnboardedProductState> status = new ArrayList<>();
        final List<PartyRole> fullListOfRoles = List.of(PartyRole.values());

        when(userInstitutionService.countUsers(institutionId, productId, fullListOfRoles, List.of(ACTIVE)))
                .thenReturn(Uni.createFrom().item(2L));

        userService.getUsersCount(institutionId, productId, roles, status).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertItem(new UsersCountResponse(institutionId, productId, fullListOfRoles, List.of(ACTIVE), 2L))
                .assertCompleted();

        verify(userInstitutionService).countUsers(institutionId, productId, fullListOfRoles, List.of(ACTIVE));
    }

    @Test
    void testGetUsersCountWithRolesAndNoStatus() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<PartyRole> roles = List.of(PartyRole.MANAGER, PartyRole.DELEGATE);
        final List<OnboardedProductState> status = new ArrayList<>();

        when(userInstitutionService.countUsers(institutionId, productId, roles, List.of(ACTIVE)))
                .thenReturn(Uni.createFrom().item(2L));

        userService.getUsersCount(institutionId, productId, roles, status).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertItem(new UsersCountResponse(institutionId, productId, roles, List.of(ACTIVE), 2L))
                .assertCompleted();

        verify(userInstitutionService).countUsers(institutionId, productId, roles, List.of(ACTIVE));
    }

    @Test
    void testGetUsersCountWithStatusAndNoRoles() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<PartyRole> roles = new ArrayList<>();
        final List<OnboardedProductState> status = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING);
        final List<PartyRole> fullListOfRoles = List.of(PartyRole.values());

        when(userInstitutionService.countUsers(institutionId, productId, fullListOfRoles, status))
                .thenReturn(Uni.createFrom().item(2L));

        userService.getUsersCount(institutionId, productId, roles, status).subscribe()
                .withSubscriber(UniAssertSubscriber.create())
                .assertItem(new UsersCountResponse(institutionId, productId, fullListOfRoles, status, 2L))
                .assertCompleted();

        verify(userInstitutionService).countUsers(institutionId, productId, fullListOfRoles, status);
    }

    @Test
    void checkUser_shouldReturnTrue_whenUserExistsAndIsValid() {
        String fiscalCode = "fiscalCode";
        String institutionId = "institutionId";
        String productId = "productId";
        UserResource userDto = new UserResource();
        userDto.setId(userId);

        Mockito.when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().item(userDto));

        Mockito.when(userInstitutionService.existsValidUserProduct(
                        userId.toString(), institutionId, productId, PermissionTypeEnum.ANY, List.of(ACTIVE, SUSPENDED)))
                .thenReturn(Uni.createFrom().item(Boolean.TRUE));

        Uni<Boolean> result = userService.checkUser(fiscalCode, institutionId, productId);

        assertTrue(result.await().indefinitely());
    }

    @Test
    void checkUser_shouldReturnFalse_whenUserNotFound() {
        String fiscalCode = "fiscalCode";
        String institutionId = "institutionId";
        String productId = "productId";
        WebApplicationException notFoundException = new WebApplicationException(Response.status(404).build());

        Mockito.when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().failure(notFoundException));

        Uni<Boolean> result = userService.checkUser(fiscalCode, institutionId, productId);

        assertFalse(result.await().indefinitely());
    }

    @Test
    void checkUser_shouldPropagateOtherErrors() {
        String fiscalCode = "fiscalCode";
        String institutionId = "institutionId";
        String productId = "productId";
        RuntimeException genericException = new RuntimeException("Unexpected error");

        Mockito.when(userRegistryApi.searchUsingPOST(any(), any()))
                .thenReturn(Uni.createFrom().failure(genericException));

        UniAssertSubscriber<Boolean> subscriber =
                userService.checkUser(fiscalCode, institutionId, productId)
                        .subscribe()
                        .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(RuntimeException.class);
    }

    @Test
    void testSendMailOtp() {
        NameCertifiableSchema nameSchema = NameCertifiableSchema.builder().value("name").build();
        UserResource user = UserResource.builder().name(nameSchema).build();
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(user));

        when(userNotificationService.sendOtpNotification(
                anyString(),
                anyString(),
                anyString())
        ).thenReturn(Uni.createFrom().voidItem());

        var subscriber = userService.sendEmailOtp("userId", "email", "123456")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitItem();

        verify(userNotificationService, times(1)).sendOtpNotification(
                eq("email"),
                eq(user.getName().getValue()),
                eq("123456")
        );
    }

}