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
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.controller.response.UserResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.mapper.UserMapperImpl;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import jakarta.inject.Inject;
import org.apache.http.HttpStatus;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfLocalDate;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER;
import static it.pagopa.selfcare.user.constant.CustomError.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class UserServiceTest {

    @Inject
    private UserService userService;

    @InjectMock
    private UserInstitutionService userInstitutionService;

    @InjectMock
    private UserInfoService userInfoService;

    @RestClient
    @InjectMock
    private UserApi userRegistryApi;


    @Spy
    private UserMapper userMapper = new UserMapperImpl();

    @InjectMock
    private ProductService productService;

    private static UserResource userResource;
    private static UserInstitution userInstitution;

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
        userResource.setWorkContacts(Map.of("userMailUuid", workContactResource));

        userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId("institutionId");
        userInstitution.setUserMailUuid("userMailUuid");
        userInstitution.setInstitutionRootName("institutionRootName");
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        userInstitution.setProducts(List.of(product));
    }
    
    @Test
    void getUsersEmailsTest() {

        when(userInstitutionService.findAllWithFilter(any())).thenReturn(Multi.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<List<String>> subscriber = userService
                .getUsersEmails("institutionId", "productId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        List<String> actual = subscriber.assertCompleted().awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("test@test.it", actual.get(0));
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

        UniAssertSubscriber<UserResponse> subscriber = userService.retrievePerson("test-user", "test-product", "test-institutionId").subscribe().withSubscriber(UniAssertSubscriber.create());
        assertEquals("userMail", subscriber.getItem().getEmail());
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
        when(userInstitutionService.retrieveFirstFilteredUserInstitution(any())).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.findByIdUsingGET(any(), any())).thenReturn(Uni.createFrom().failure(new WebClientApplicationException(HttpStatus.SC_NOT_FOUND)));

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

        UniAssertSubscriber<UserInfo> subscriber = userService
                .retrieveBindings(null, "test-user", null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(userInfoResponse);
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
    void deleteUserInstitutionProductFound(){
        when(userInstitutionService.deleteUserInstitutionProduct("userId", "institutionId", "productId")).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Void> subscriber = userService
                .deleteUserInstitutionProduct("userId", "institutionId", "productId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted();

    }

    @Test
    void deleteUserInstitutionProductNotFound(){
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
}
