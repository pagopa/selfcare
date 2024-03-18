package it.pagopa.selfcare.user.service;


import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.common.constraint.Assert;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.UserDataResponse;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER;
import static it.pagopa.selfcare.user.constant.CustomError.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class UserServiceTest {

    @Inject
    UserService userService;

    @InjectMock
    private UserInstitutionService userInstitutionService;

    @InjectMock
    private UserNotificationService userNotificationService;

    @InjectMock
    private UserInfoService userInfoService;

    @RestClient
    @InjectMock
    private UserApi userRegistryApi;

    @InjectMock
    private UserMapper userMapper;

    @InjectMock
    private ProductService productService;

    private static final UserResource userResource;
    private static final UserInstitution userInstitution;

    static {
        userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        CertifiableFieldResourceOfstring certifiedName = new CertifiableFieldResourceOfstring();
        certifiedName.setValue("name");
        userResource.setName(certifiedName);
        userResource.setFamilyName(certifiedName);
        userResource.setFiscalCode("taxCode");
        CertifiableFieldResourceOfstring certifiedEmail = new CertifiableFieldResourceOfstring();
        certifiedEmail.setValue("test@test.it");
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(certifiedEmail);
        userResource.setEmail(certifiedEmail);

        Map<String, WorkContactResource> map = new HashMap<>();
        map.put("userMailUuid", workContactResource);
        userResource.setWorkContacts(map);

        userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId("institutionId");
        userInstitution.setUserMailUuid("userMailUuid");
        userInstitution.setInstitutionRootName("institutionRootName");
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        List<OnboardedProduct> products = new ArrayList<>();
        products.add(product);
        userInstitution.setProducts(products);
    }

    @Test
    void getUsersEmailsTest() {

        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(userInstitution));
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
    void getUserById() {
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));
        UniAssertSubscriber<UserResource> subscriber = userService
                .getUserById("userId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        UserResource actual = subscriber.assertCompleted().awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(userResource.getId(), actual.getId());
    }


    @Test
    void getUserProductsByInstitutionTest() {
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(userInstitution));
        when(UserInstitution.find(any(), (Object) any()))
                .thenReturn(query);
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        AssertSubscriber<UserProductResponse> subscriber = userService
                .getUserProductsByInstitution("institutionId")
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        List<UserProductResponse> actual = subscriber.assertCompleted().getItems();
        assertNotNull(actual);
        assertEquals(1, actual.size());
    }

    @Test
    void testRetrievePerson() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId("test-user");
        String userMailUuId = UUID.randomUUID().toString();
        userInstitution.setUserMailUuid(userMailUuId);

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        userResource.setFiscalCode("test");
        userResource.setBirthDate(CertifiableFieldResourceOfLocalDate.builder().value(LocalDate.now()).build());
        userResource.setEmail(CertifiableFieldResourceOfstring.builder().value("test@test.com").build());
        userResource.setName(CertifiableFieldResourceOfstring.builder().value("testName").build());
        userResource.setFamilyName(CertifiableFieldResourceOfstring.builder().value("testFamilyName").build());
        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(CertifiableFieldResourceOfstring.builder().value("userMail").build());
        userResource.setWorkContacts(Map.of(userMailUuId, workContactResource));

        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<UserResource> subscriber = userService.retrievePerson("test-user", "test-product", "test-institutionId").subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }

    @Test
    void testRetrievePersonFailsWhenUserIsNotPresent() {
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().nullItem());

        UniAssertSubscriber<UserResource> subscriber = userService
                .retrievePerson("test-user", "test-product", "test-institutionId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);
    }

    @Test
    void testRetrievePersonFailsWhenPdvFails() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId("test-user");
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().failure(new WebClientApplicationException(HttpStatus.SC_NOT_FOUND)));

        UniAssertSubscriber<UserResource> subscriber = userService.retrievePerson("test-user", "test-product", "test-institutionId").subscribe().withSubscriber(UniAssertSubscriber.create());

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

        UniAssertSubscriber<UserInfo> subscriber = userService
                .retrieveBindings(null, "test-user", null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(userInfoResponse);
    }

    @Test
    void searchUserByFiscalCode(){
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        UniAssertSubscriber<UserResource> subscriber = userService
                .searchUserByFiscalCode("userId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        UserResource actual = subscriber.assertCompleted().awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(userResource.getId(), actual.getId());

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

        String[] states = {"ACTIVE"};

        UniAssertSubscriber<UserInfo> subscriber = userService
                .retrieveBindings("test-institutionId", "test-user", states)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);

    }

    @Test
    void updateUserStatusWithOptionalFilter(){
        when(userInstitutionService
                .updateUserStatusWithOptionalFilterByInstitutionAndProduct("userId", "institutionId", "prod-pagopa", MANAGER, null, OnboardedProductState.ACTIVE)).thenReturn(Uni.createFrom().item(1L));

        UniAssertSubscriber<Void> subscriber = userService
                .updateUserStatusWithOptionalFilter("userId", "institutionId", "prod-pagopa", MANAGER, null, OnboardedProductState.ACTIVE)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();
    }

    @Test
    void updateUserStatusWithOptionalFilterUserNotFound(){
        when(userInstitutionService
                .updateUserStatusWithOptionalFilterByInstitutionAndProduct("userId", "institutionId", "prod-pagopa", MANAGER, null, OnboardedProductState.ACTIVE)).thenReturn(Uni.createFrom().item(0L));

        UniAssertSubscriber<Void> subscriber = userService
                .updateUserStatusWithOptionalFilter("userId", "institutionId", "prod-pagopa", MANAGER, null, OnboardedProductState.ACTIVE)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class, USER_TO_UPDATE_NOT_FOUND.getMessage());
    }

    @Test
    void updateUserStatusWithOptionalFilterInvalidRequest(){
        UniAssertSubscriber<Void> subscriber = userService
                .updateUserStatusWithOptionalFilter("userId", "institutionId", null, MANAGER, null, null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class, STATUS_IS_MANDATORY.getMessage());
    }

    @Test
    void retrieveUsersTest() {
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
    void findAllUserInstitutionsPaged() {
        when(userInstitutionService.paginatedFindAllWithFilter(anyMap(), anyInt(), anyInt())).thenReturn(Multi.createFrom().item(userInstitution));
        AssertSubscriber<UserInstitutionResponse> subscriber = userService
                .findPaginatedUserInstitutions("institutionId", "userId", null, null, null, null, 0, 100)
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        List<UserInstitutionResponse> actual = subscriber.assertCompleted().getItems();
        Assert.assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(userInstitution.getUserId(), actual.get(0).getUserId());
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
    void findAllByIds(){
        //given
        List<String> userIds = List.of("userId");
        when(userInstitutionService.findAllWithFilter(any()))
                .thenReturn(Multi.createFrom().item(userInstitution));
        //when
        UniAssertSubscriber<List<UserInstitutionResponse>> subscriber = userService.findAllByIds(userIds)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        //then
        List<UserInstitutionResponse> users=subscriber.assertCompleted().getItem();
        assertFalse(users.isEmpty());
        assertEquals(1, users.size());
    }

    @Test
    void updateUserCreatedAtUserNotFound(){
        final String productId = "productId";
        final String userId = "userId";
        final String institutionId = "institutionId";
        final LocalDateTime now = LocalDateTime.now();
        when(userInstitutionService
                .updateUserCreatedAtByInstitutionAndProduct(institutionId, List.of(userId), productId, now)).thenReturn(Uni.createFrom().item(0L));

        UniAssertSubscriber<Void> subscriber = userService
                .updateUserProductCreatedAt(institutionId, List.of(userId), productId, now)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class, USERS_TO_UPDATE_NOT_FOUND.getMessage());
    }

    @Test
    void updateUserProductCreatedAt(){
        when(userInstitutionService
                .updateUserCreatedAtByInstitutionAndProduct(anyString(), any(), anyString(), any()))
                .thenReturn(Uni.createFrom().item(1L));

        UniAssertSubscriber<Void> subscriber = userService
                .updateUserProductCreatedAt("institutionId", List.of("userId"), "productId", LocalDateTime.now())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }

    @Test
    void findPaginatedUserNotificationToSend() {
        when(userInstitutionService.paginatedFindAllWithFilter(any(), any(), any()))
                .thenReturn(Multi.createFrom().item(userInstitution));
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
                .thenReturn(Multi.createFrom().item(userInstitution));
        UserResource userResource = mock(UserResource.class);
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<List<UserNotificationToSend>> subscriber = userService
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
                        "userId", "institutionId", "productId", null, null, OnboardedProductState.ACTIVE))
                .thenReturn(Uni.createFrom().item(1L));


        when(userNotificationService.sendEmailNotification(
                any(UserResource.class),
                any(UserInstitution.class),
                any(Product.class),
                any(),
                anyString(),
                anyString())
        ).thenReturn(Uni.createFrom().voidItem());

        when(userNotificationService.sendKafkaNotification(
                any(UserNotificationToSend.class),
                any())
        ).thenReturn(Uni.createFrom().nullItem());

        var subscriber = userService.updateUserProductStatus("userId", "institutionId", "productId", OnboardedProductState.ACTIVE,
                        LoggedUser.builder().build())
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitItem();

        verify(userNotificationService, times(1)).sendEmailNotification(
                any(UserResource.class),
                any(UserInstitution.class),
                any(Product.class),
                any(OnboardedProductState.class),
                eq(null),
                eq(null)
        );
        verify(userNotificationService, times(1)).sendKafkaNotification(
                any(UserNotificationToSend.class),
                any()
        );
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_Success() {
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        createUserDto.setUser(user);

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(any(), any())).thenReturn(Uni.createFrom().item(Response.ok().build()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().voidItem());

        // Call the method
        UniAssertSubscriber<Void> subscriber = userService.createOrUpdateUser(createUserDto)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.assertCompleted();
        verify(userRegistryApi).updateUsingPATCH(any(), any());
        verify(userInstitutionService).persistOrUpdate(any());
    }

    @Test
    void testCreateOrUpdateUser_CreateUser_Success() {
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        createUserDto.setUser(user);
        CreateUserDto.Product product = new CreateUserDto.Product();
        product.setProductId("productId");
        createUserDto.setProduct(product);

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().failure(new WebClientApplicationException(HttpStatus.SC_NOT_FOUND)));
        when(userRegistryApi.saveUsingPATCH(any())).thenReturn(Uni.createFrom().item(UserId.builder().id(UUID.randomUUID()).build()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().voidItem());

        // Call the method
        UniAssertSubscriber<Void> subscriber = userService.createOrUpdateUser(createUserDto)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.assertCompleted();
        verify(userRegistryApi).saveUsingPATCH(any());
        verify(userInstitutionService).persistOrUpdate(any());
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_UserRegistryUpdateFailed() {
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        createUserDto.setUser(user);

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(any(), any())).thenReturn(Uni.createFrom().failure(new RuntimeException()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().voidItem());

        // Call the method
        UniAssertSubscriber<Void> subscriber = userService.createOrUpdateUser(createUserDto)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.assertFailedWith(RuntimeException.class);
        verify(userRegistryApi).updateUsingPATCH(any(), any());
        verify(userInstitutionService, never()).persistOrUpdate(any());
    }

    @Test
    void testCreateOrUpdateUser_UpdateUser_UserInstitutionUpdateFailed() {
        // Prepare test data
        CreateUserDto createUserDto = new CreateUserDto();
        CreateUserDto.User user = new CreateUserDto.User();
        user.setFiscalCode("fiscalCode");
        createUserDto.setUser(user);

        // Mock external dependencies
        when(userRegistryApi.searchUsingPOST(any(), any())).thenReturn(Uni.createFrom().item(userResource));
        when(userInstitutionService.findByUserIdAndInstitutionId(any(), any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(any(), any())).thenReturn(Uni.createFrom().item(Response.ok().build()));
        when(userInstitutionService.persistOrUpdate(any())).thenReturn(Uni.createFrom().failure(new RuntimeException()));

        // Call the method
        UniAssertSubscriber<Void> subscriber = userService.createOrUpdateUser(createUserDto)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Verify the result
        subscriber.assertFailedWith(RuntimeException.class);
        verify(userRegistryApi).updateUsingPATCH(any(), any());
        verify(userInstitutionService).persistOrUpdate(any());
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
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(anyMap())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        // Call the method
        AssertSubscriber<UserDataResponse> subscriber = userService.retrieveUsersData(institutionId, personId, roles, states, products, productRoles, userUuid)
                .subscribe().withSubscriber(AssertSubscriber.create());

        // Verify the result
        subscriber.assertCompleted().getItems().forEach(actual -> {
            assertNotNull(actual);
            assertEquals(personId, actual.getId().equals("test-person"));
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
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().item(userResource));

        // Call the method
        AssertSubscriber<UserDataResponse> subscriber = userService.retrieveUsersData(institutionId, personId, roles, states, products, productRoles, userUuid)
                .subscribe().withSubscriber(AssertSubscriber.create());

        // Verify the result
        subscriber.assertCompleted().getItems().forEach(actual -> {
            assertNotNull(actual);
            assertEquals(personId, actual.getId().equals(userUuid));
            assertEquals(institutionId, "test-institution");
        });

        // Verify the interactions
        verify(userInstitutionService).retrieveFirstFilteredUserInstitution(anyMap());
        verify(userInstitutionService).findAllWithFilter(any());
        verify(userRegistryApi).findByIdUsingGET(any(), any());
    }

}