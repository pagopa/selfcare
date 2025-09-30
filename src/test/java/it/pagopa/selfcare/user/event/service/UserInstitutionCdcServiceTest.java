package it.pagopa.selfcare.user.event.service;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.client.EventHubFdRestClient;
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.event.UserInstitutionCdcService;
import it.pagopa.selfcare.user.event.client.InternalDelegationApiClient;
import it.pagopa.selfcare.user.event.client.InternalUserApiClient;
import it.pagopa.selfcare.user.event.client.InternalUserGroupApiClient;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.repository.UserInstitutionRepository;
import it.pagopa.selfcare.user.model.FdUserNotificationToSend;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.bson.BsonDocument;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.internal_json.model.*;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.selfcare.user.event.UserInstitutionCdcService.USERS_FIELD_LIST_WITHOUT_FISCAL_CODE;
import static it.pagopa.selfcare.user.model.NotificationUserType.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class UserInstitutionCdcServiceTest {

    @Inject
    UserInstitutionCdcService userInstitutionCdcService;

    @RestClient
    @InjectMock
    UserApi userRegistryApi;

    @RestClient
    @InjectMock
    InternalDelegationApiClient delegationApi;

    @RestClient
    @InjectMock
    InternalUserApiClient userApi;

    @RestClient
    @InjectMock
    InternalUserGroupApiClient userGroupApi;

    @RestClient
    @InjectMock
    EventHubRestClient eventHubRestClient;

    @RestClient
    @InjectMock
    EventHubFdRestClient eventHubFdRestClient;

    @InjectMock
    UserInstitutionRepository userInstitutionRepository;

    @InjectMock
    ProductService productService;

    @Test
    void consumerToSendScUserEvent() {
        UserInstitution userInstitution = dummyUserInstitution(false, null);
        ChangeStreamDocument<UserInstitution> document = Mockito.mock(ChangeStreamDocument.class);
        when(document.getFullDocument()).thenReturn(userInstitution);
        when(document.getDocumentKey()).thenReturn(new BsonDocument());

        UserResource userResource = dummyUserResource();
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));

        userInstitutionCdcService.consumerToSendScUserEvent(document);
        verify(userRegistryApi, times(1)).
                findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId());
        verify(eventHubRestClient, times(2)).
                sendMessage(any(UserNotificationToSend.class));
    }

    @Test
    void consumerToSendUserEventForFDSendACTIVE_USER() {
        UserInstitution userInstitution = dummyUserInstitution(true, OnboardedProductState.ACTIVE);
        ChangeStreamDocument<UserInstitution> document = Mockito.mock(ChangeStreamDocument.class);
        when(document.getFullDocument()).thenReturn(userInstitution);
        when(document.getDocumentKey()).thenReturn(new BsonDocument());

        UserResource userResource = dummyUserResource();
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));
        ArgumentCaptor<FdUserNotificationToSend> argumentCaptor = ArgumentCaptor.forClass(FdUserNotificationToSend.class);
        when(eventHubFdRestClient.sendMessage(argumentCaptor.capture()))
                .thenReturn(Uni.createFrom().nullItem());

        userInstitutionCdcService.consumerToSendUserEventForFD(document, false);
        verify(userRegistryApi, times(1)).
                findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId());
        verify(eventHubFdRestClient, times(1)).
                sendMessage(any(FdUserNotificationToSend.class));
        Assertions.assertEquals(ACTIVE_USER, argumentCaptor.getValue().getType());
    }

    @Test
    void consumerToSendUserEventForFDSendACTIVE_USER_mailChanged() {
        UserInstitution userInstitution = dummyUserInstitution(false, OnboardedProductState.ACTIVE);
        ChangeStreamDocument<UserInstitution> document = Mockito.mock(ChangeStreamDocument.class);
        when(document.getFullDocument()).thenReturn(userInstitution);
        when(document.getDocumentKey()).thenReturn(new BsonDocument());

        UserResource userResource = dummyUserResource();
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));
        ArgumentCaptor<FdUserNotificationToSend> argumentCaptor = ArgumentCaptor.forClass(FdUserNotificationToSend.class);
        when(eventHubFdRestClient.sendMessage(argumentCaptor.capture()))
                .thenReturn(Uni.createFrom().nullItem());

        userInstitutionCdcService.consumerToSendUserEventForFD(document, true);
        verify(userRegistryApi, times(1)).
                findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId());
        verify(eventHubFdRestClient, times(2)).
                sendMessage(any(FdUserNotificationToSend.class));
        Assertions.assertEquals(ACTIVE_USER, argumentCaptor.getValue().getType());
    }

    @Test
    void consumerToSendUserEventForFDSendSUSPEND_USER() {
        UserInstitution userInstitution = dummyUserInstitution(true, OnboardedProductState.SUSPENDED);
        ChangeStreamDocument<UserInstitution> document = Mockito.mock(ChangeStreamDocument.class);
        when(document.getFullDocument()).thenReturn(userInstitution);
        when(document.getDocumentKey()).thenReturn(new BsonDocument());

        UserResource userResource = dummyUserResource();
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));
        ArgumentCaptor<FdUserNotificationToSend> argumentCaptor = ArgumentCaptor.forClass(FdUserNotificationToSend.class);

        when(eventHubFdRestClient.sendMessage(argumentCaptor.capture()))
                .thenReturn(Uni.createFrom().nullItem());

        userInstitutionCdcService.consumerToSendUserEventForFD(document, false);
        verify(userRegistryApi, times(1)).
                findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId());
        verify(eventHubFdRestClient, times(1)).
                sendMessage(any(FdUserNotificationToSend.class));
        Assertions.assertEquals(SUSPEND_USER, argumentCaptor.getValue().getType());
    }

    @Test
    void consumerToSendUserEventForFDSendDELETE_USER() {
        UserInstitution userInstitution = dummyUserInstitution(true, OnboardedProductState.DELETED);
        ChangeStreamDocument<UserInstitution> document = Mockito.mock(ChangeStreamDocument.class);
        when(document.getFullDocument()).thenReturn(userInstitution);
        when(document.getDocumentKey()).thenReturn(new BsonDocument());

        UserResource userResource = dummyUserResource();
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));
        ArgumentCaptor<FdUserNotificationToSend> argumentCaptor = ArgumentCaptor.forClass(FdUserNotificationToSend.class);
        when(eventHubFdRestClient.sendMessage(argumentCaptor.capture()))
                .thenReturn(Uni.createFrom().nullItem());

        userInstitutionCdcService.consumerToSendUserEventForFD(document, false);
        verify(userRegistryApi, times(1)).
                findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId());
        verify(eventHubFdRestClient, times(1)).
                sendMessage(any(FdUserNotificationToSend.class));
        Assertions.assertEquals(DELETE_USER, argumentCaptor.getValue().getType());
    }

    @Test
    void consumerToSendUserEventForFDNotSend() {
        UserInstitution userInstitution = dummyUserInstitution(false, null);
        ChangeStreamDocument<UserInstitution> document = Mockito.mock(ChangeStreamDocument.class);
        when(document.getFullDocument()).thenReturn(userInstitution);
        when(document.getDocumentKey()).thenReturn(new BsonDocument());

        UserResource userResource = dummyUserResource();
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));

        userInstitutionCdcService.consumerToSendUserEventForFD(document, false);
        verify(userRegistryApi, times(1)).
                findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId());
        verify(eventHubFdRestClient, times(0)).
                sendMessage(any(FdUserNotificationToSend.class));
    }


    UserResource dummyUserResource() {
        UserResource userResource = new UserResource();

        return userResource;
    }

    UserInstitution dummyUserInstitution(boolean sendForFd, OnboardedProductState state) {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserMailUpdatedAt(OffsetDateTime.of(2023, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC));
        if (sendForFd) {
            userInstitution.setProducts(List.of(dummyOnboardedProduct("example-1", state, 2, "prod-fd"),
                    dummyOnboardedProduct("example-2", OnboardedProductState.ACTIVE, 1, "prod-io")));
        } else {
            userInstitution.setProducts(List.of(dummyOnboardedProduct("example-1", OnboardedProductState.ACTIVE, 2, "prod-io"),
                    dummyOnboardedProduct("example-2", OnboardedProductState.ACTIVE, 1, "prod-fd")));
        }
        return userInstitution;

    }

    OnboardedProduct dummyOnboardedProduct(String productRole, OnboardedProductState state, int day, String productId) {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setProductRole(productRole);
        onboardedProduct.setCreatedAt(OffsetDateTime.of(2024, 1, day, 0, 0, 0, 0, ZoneOffset.UTC));
        onboardedProduct.setUpdatedAt(OffsetDateTime.of(2024, 1, day, 0, 0, 0, 0, ZoneOffset.UTC));
        onboardedProduct.setStatus(state);
        return onboardedProduct;
    }


    @Test
    void propagateDocumentToConsumers_withChangeUserMailFalse() {
        ChangeStreamDocument<UserInstitution> document = mock(ChangeStreamDocument.class);

        Multi<ChangeStreamDocument<UserInstitution>> publisher = Multi.createFrom().item(document);

        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(new ObjectId());
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("prod-io");
        product.setStatus(OnboardedProductState.ACTIVE);
        userInstitution.setProducts(List.of(product));

        UserResource userResource = dummyUserResource();
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));

        when(document.getFullDocument()).thenReturn(userInstitution);
        when(document.getDocumentKey()).thenReturn(new BsonDocument());

        userInstitutionCdcService.propagateDocumentToConsumers(document, publisher);
        verify(eventHubFdRestClient, times(0)).sendMessage(any(FdUserNotificationToSend.class));
        verify(eventHubRestClient, times(1)).sendMessage(any(UserNotificationToSend.class));
        verify(userInstitutionRepository, times(1)).updateUser(any());
    }

    @Test
    void propagateDocumentToConsumers_withChangeUserMailWithFd() {
        ChangeStreamDocument<UserInstitution> document = mock(ChangeStreamDocument.class);

        Multi<ChangeStreamDocument<UserInstitution>> publisher = Multi.createFrom().item(document);

        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(new ObjectId());
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("prod-fd");
        product.setStatus(OnboardedProductState.ACTIVE);
        userInstitution.setProducts(List.of(product));
        userInstitution.setUserMailUpdatedAt(OffsetDateTime.of(LocalDate.now(), LocalTime.now(), ZoneOffset.UTC));

        UserResource userResource = dummyUserResource();
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));

        when(document.getFullDocument()).thenReturn(userInstitution);
        when(document.getDocumentKey()).thenReturn(new BsonDocument());

        userInstitutionCdcService.propagateDocumentToConsumers(document, publisher);
        verify(eventHubFdRestClient, times(1)).sendMessage(any(FdUserNotificationToSend.class));
        verify(eventHubRestClient, times(1)).sendMessage(any(UserNotificationToSend.class));
        verify(userInstitutionRepository, times(1)).updateUser(any());
    }


    @Test
    void propagateDocumentToConsumers_withChangeUserMailTrueWithoutFd() {

        ChangeStreamDocument<UserInstitution> document = mock(ChangeStreamDocument.class);

        Multi<ChangeStreamDocument<UserInstitution>> publisher = Multi.createFrom().item(document);

        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(new ObjectId());
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("prod-io");
        product.setStatus(OnboardedProductState.ACTIVE);
        userInstitution.setProducts(List.of(product));
        userInstitution.setUserMailUpdatedAt(OffsetDateTime.of(LocalDate.now(), LocalTime.now(), ZoneOffset.UTC));

        UserResource userResource = dummyUserResource();
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId()))
                .thenReturn(Uni.createFrom().item(userResource));

        when(document.getFullDocument()).thenReturn(userInstitution);
        when(document.getDocumentKey()).thenReturn(new BsonDocument());

        userInstitutionCdcService.propagateDocumentToConsumers(document, publisher);
        verify(eventHubFdRestClient, times(0)).sendMessage(any(FdUserNotificationToSend.class));
        verify(eventHubRestClient, times(1)).sendMessage(any(UserNotificationToSend.class));
        verify(userInstitutionRepository, times(1)).updateUser(any());
    }

    @Test
    void consumerToAddOnAggregatesTest() {
        final String parentInstitutionId = "parentInstitutionId";

        final UserInstitution userInstitution = new UserInstitution();
        userInstitution.setInstitutionId(parentInstitutionId);
        userInstitution.setUserId(UUID.randomUUID().toString());
        final OnboardedProduct prod1 = new OnboardedProduct();
        prod1.setProductId("prod-interop");
        prod1.setStatus(OnboardedProductState.ACTIVE);
        prod1.setRole(PartyRole.SUB_DELEGATE);
        prod1.setProductRole("admin");
        final OnboardedProduct prod2 = new OnboardedProduct();
        prod2.setProductId("prod-pn");
        prod2.setStatus(OnboardedProductState.SUSPENDED);
        prod2.setRole(PartyRole.DELEGATE);
        prod2.setProductRole("admin");
        prod2.setToAddOnAggregates(true);
        final OnboardedProduct prod3 = new OnboardedProduct();
        prod3.setProductId("prod-io");
        prod3.setStatus(OnboardedProductState.ACTIVE);
        prod3.setRole(PartyRole.MANAGER);
        prod3.setProductRole("admin");
        prod3.setToAddOnAggregates(true);
        final OnboardedProduct prod4 = new OnboardedProduct();
        prod4.setProductId("prod-pagopa");
        prod4.setStatus(OnboardedProductState.ACTIVE);
        prod4.setRole(PartyRole.OPERATOR);
        prod4.setProductRole("operator");
        prod4.setToAddOnAggregates(true);
        userInstitution.setProducts(List.of(prod1, prod2, prod3, prod4));
        userInstitution.setUserMailUuid("userMailUuid");

        final DelegationWithPaginationResponse delegationsIo1 = new DelegationWithPaginationResponse();
        delegationsIo1.setDelegations(List.of(createDelegationResponse(DelegationResponse.StatusEnum.ACTIVE, DelegationResponse.TypeEnum.EA)));
        delegationsIo1.setPageInfo(PageInfo.builder().pageNo(0L).pageSize(1L).totalPages(2L).totalElements(2L).build());
        when(delegationApi.getDelegationsUsingGET2(null, parentInstitutionId, "prod-io", null, null, null, 0, null)).thenReturn(Uni.createFrom().item(delegationsIo1));
        final DelegationWithPaginationResponse delegationsIo2 = new DelegationWithPaginationResponse();
        delegationsIo2.setDelegations(List.of(createDelegationResponse(DelegationResponse.StatusEnum.ACTIVE, DelegationResponse.TypeEnum.EA)));
        delegationsIo2.setPageInfo(PageInfo.builder().pageNo(1L).pageSize(1L).totalPages(2L).totalElements(2L).build());
        when(delegationApi.getDelegationsUsingGET2(null, parentInstitutionId, "prod-io", null, null, null, 1, null)).thenReturn(Uni.createFrom().item(delegationsIo2));

        final DelegationWithPaginationResponse delegationsPagoPa = new DelegationWithPaginationResponse();
        delegationsPagoPa.setDelegations(List.of(
                createDelegationResponse(DelegationResponse.StatusEnum.ACTIVE, DelegationResponse.TypeEnum.EA),
                createDelegationResponse(DelegationResponse.StatusEnum.ACTIVE, DelegationResponse.TypeEnum.PT),
                createDelegationResponse(DelegationResponse.StatusEnum.DELETED, DelegationResponse.TypeEnum.EA)
        ));
        delegationsPagoPa.setPageInfo(PageInfo.builder().pageNo(0L).pageSize(1000L).totalPages(1L).totalElements(3L).build());
        when(delegationApi.getDelegationsUsingGET2(null, parentInstitutionId, "prod-pagopa", null, null, null, 0, null)).thenReturn(Uni.createFrom().item(delegationsPagoPa));

        final Product p = new Product();
        final ProductRoleInfo pri = new ProductRoleInfo();
        final ProductRole pr = new ProductRole();
        pr.setCode("admin");
        pri.setRoles(List.of(pr));
        p.setRoleMappings(Map.of(PartyRole.ADMIN_EA, pri));
        when(productService.getProduct(any())).thenReturn(p);

        when(userApi.createUserByUserId(any(), any())).thenReturn(Uni.createFrom().item(Response.status(Response.Status.OK).build()));
        when(userGroupApi.addMembersToUserGroupWithParentInstitutionIdUsingPUT(any())).thenReturn(Uni.createFrom().item(Response.status(Response.Status.OK).build()));

        userInstitutionCdcService.consumerToAddOnAggregates(userInstitution);
        verify(delegationApi, times(3)).getDelegationsUsingGET2(any(), any(), any(), any(), any(), any(), any(), any());
        verify(userApi, times(3)).createUserByUserId(any(), any());
        verify(userGroupApi, times(2)).addMembersToUserGroupWithParentInstitutionIdUsingPUT(any());
    }

    private DelegationResponse createDelegationResponse(DelegationResponse.StatusEnum status, DelegationResponse.TypeEnum type) {
        final DelegationResponse delegationResponse = new DelegationResponse();
        delegationResponse.setStatus(status);
        delegationResponse.setType(type);
        return delegationResponse;
    }

}
