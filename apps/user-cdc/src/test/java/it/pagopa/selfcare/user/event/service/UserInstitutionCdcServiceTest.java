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
        // no toAddOnAggregates
        final OnboardedProduct prod1 = createOnboardedProduct("roleId1", "prod-interop", OnboardedProductState.ACTIVE,
                PartyRole.SUB_DELEGATE, "admin", null);
        // no delegations
        final OnboardedProduct prod2 = createOnboardedProduct("roleId2", "prod-pn", OnboardedProductState.SUSPENDED,
                PartyRole.DELEGATE, "admin", true);
        // to propagate
        final OnboardedProduct prod3 = createOnboardedProduct("roleId3", "prod-io", OnboardedProductState.ACTIVE,
                PartyRole.MANAGER, "admin", true);
        // to propagate
        final OnboardedProduct prod3bis = createOnboardedProduct("roleId3bis", "prod-io", OnboardedProductState.DELETED,
                PartyRole.DELEGATE, "admin", true);
        // to propagate (but no group)
        final OnboardedProduct prod4 = createOnboardedProduct("roleId4", "prod-pagopa", OnboardedProductState.ACTIVE,
                PartyRole.OPERATOR, "operator", true);
        // no roleId
        final OnboardedProduct prod5 = createOnboardedProduct(null, "prod-pagopa", OnboardedProductState.ACTIVE,
                PartyRole.OPERATOR, "operator", true);
        // Parent UserInstitution
        final UserInstitution parentUserInstitution = createUserInstitution("parentInstitutionId",
                UUID.randomUUID().toString(), List.of(prod1, prod2, prod3, prod3bis, prod4, prod5), "userMailUuid");

        // Delegations IO page 1
        final DelegationWithPaginationResponse delegationsIo1 = new DelegationWithPaginationResponse();
        delegationsIo1.setDelegations(List.of(createDelegationResponse(DelegationResponse.StatusEnum.ACTIVE, DelegationResponse.TypeEnum.EA)));
        delegationsIo1.setPageInfo(PageInfo.builder().pageNo(0L).pageSize(1L).totalPages(2L).totalElements(2L).build());
        when(delegationApi.getDelegationsUsingGET2(null, parentUserInstitution.getInstitutionId(), "prod-io", null, null, null, 0, null)).thenReturn(Uni.createFrom().item(delegationsIo1));

        // Delegations IO page 2
        final DelegationWithPaginationResponse delegationsIo2 = new DelegationWithPaginationResponse();
        delegationsIo2.setDelegations(List.of(createDelegationResponse(DelegationResponse.StatusEnum.ACTIVE, DelegationResponse.TypeEnum.EA)));
        delegationsIo2.setPageInfo(PageInfo.builder().pageNo(1L).pageSize(1L).totalPages(2L).totalElements(2L).build());
        when(delegationApi.getDelegationsUsingGET2(null, parentUserInstitution.getInstitutionId(), "prod-io", null, null, null, 1, null)).thenReturn(Uni.createFrom().item(delegationsIo2));

        // Delegations PN
        final DelegationWithPaginationResponse delegationsPn = new DelegationWithPaginationResponse();
        delegationsPn.setDelegations(List.of());
        delegationsPn.setPageInfo(PageInfo.builder().pageNo(0L).pageSize(0L).totalPages(0L).totalElements(0L).build());
        when(delegationApi.getDelegationsUsingGET2(null, parentUserInstitution.getInstitutionId(), "prod-pn", null, null, null, 0, null)).thenReturn(Uni.createFrom().item(delegationsPn));

        // Delegations PagoPA
        final DelegationWithPaginationResponse delegationsPagoPa = new DelegationWithPaginationResponse();
        delegationsPagoPa.setDelegations(List.of(
                createDelegationResponse(DelegationResponse.StatusEnum.ACTIVE, DelegationResponse.TypeEnum.EA),
                createDelegationResponse(DelegationResponse.StatusEnum.ACTIVE, DelegationResponse.TypeEnum.PT),
                createDelegationResponse(DelegationResponse.StatusEnum.DELETED, DelegationResponse.TypeEnum.EA)
        ));
        delegationsPagoPa.setPageInfo(PageInfo.builder().pageNo(0L).pageSize(1000L).totalPages(1L).totalElements(3L).build());
        when(delegationApi.getDelegationsUsingGET2(null, parentUserInstitution.getInstitutionId(), "prod-pagopa", null, null, null, 0, null)).thenReturn(Uni.createFrom().item(delegationsPagoPa));

        // Product Service
        final Product p = new Product();
        final ProductRoleInfo pri = new ProductRoleInfo();
        final ProductRole pr = new ProductRole();
        pr.setCode("admin");
        pri.setRoles(List.of(pr));
        p.setRoleMappings(Map.of(PartyRole.ADMIN_EA, pri));
        when(productService.getProduct(any())).thenReturn(p);

        // Updated UserInstitution for prod3
        final UserInstitution updatedUserInstitutionForProd3 = createUserInstitution("aggregateInstitutionId",
                parentUserInstitution.getUserId(), List.of(
                        createOnboardedProduct(null, "prod-io", OnboardedProductState.DELETED,
                                PartyRole.MANAGER, "admin", null),
                        prod3
                ), parentUserInstitution.getUserMailUuid());

        // Updated UserInstitution for prod3Bis
        final UserInstitution updatedUserInstitutionForProd3Bis = createUserInstitution("aggregateInstitutionId",
                parentUserInstitution.getUserId(), List.of(
                        prod3bis,
                        createOnboardedProduct(null, "prod-y", OnboardedProductState.SUSPENDED,
                                PartyRole.DELEGATE, "admin", null)
                ), parentUserInstitution.getUserMailUuid());

        // Updated UserInstitution for prod4
        final UserInstitution updatedUserInstitutionForProd4 = createUserInstitution("aggregateInstitutionId",
                parentUserInstitution.getUserId(), List.of(prod4), parentUserInstitution.getUserMailUuid());

        when(userInstitutionRepository.propagateUserToAggregate(any(), eq(prod3), any(), any(), any(), any())).thenReturn(Uni.createFrom().item(updatedUserInstitutionForProd3));
        when(userInstitutionRepository.propagateUserToAggregate(any(), eq(prod3bis), any(), any(), any(), any())).thenReturn(Uni.createFrom().item(updatedUserInstitutionForProd3Bis));
        when(userInstitutionRepository.propagateUserToAggregate(any(), eq(prod4), any(), any(), any(), any())).thenReturn(Uni.createFrom().item(updatedUserInstitutionForProd4));
        when(userGroupApi.addMembersToUserGroupWithParentInstitutionIdUsingPUT(any())).thenReturn(Uni.createFrom().item(Response.status(Response.Status.OK).build()));
        when(userGroupApi.deleteMembersFromUserGroupWithParentInstitutionIdUsingDELETE(any())).thenReturn(Uni.createFrom().item(Response.status(Response.Status.OK).build()));

        userInstitutionCdcService.consumerToAddOnAggregates(parentUserInstitution);

        // 4 products valid for propagation -> 1 call for prod2, 2 call for prod3 (2 pages), 2 call for prod3Bis (2 pages), 1 call for prod4
        verify(delegationApi, times(6)).getDelegationsUsingGET2(any(), any(), any(), any(), any(), any(), any(), any());
        // prod2 has no delegations -> we propagate only prod3 (on 2 aggregates), prod3Bis (on 2 aggregates) and prod4 (on 1 aggregate)
        verify(userInstitutionRepository, times(5)).propagateUserToAggregate(any(), any(), any(), any(), any(), any());
        // prod4 is prod-pagopa -> propagation to group is disabled
        // prod3 with ACTIVE role for prod-io -> we keep user inside group (2 calls for 2 aggregates)
        verify(userGroupApi, times(2)).addMembersToUserGroupWithParentInstitutionIdUsingPUT(any());
        // prod3Bis without ACTIVE / SUSPENDED roles for prod-io -> we remove user from group (2 calls for 2 aggregates)
        verify(userGroupApi, times(2)).deleteMembersFromUserGroupWithParentInstitutionIdUsingDELETE(any());
    }

    private DelegationResponse createDelegationResponse(DelegationResponse.StatusEnum status, DelegationResponse.TypeEnum type) {
        final DelegationResponse delegationResponse = new DelegationResponse();
        delegationResponse.setStatus(status);
        delegationResponse.setType(type);
        return delegationResponse;
    }

    private OnboardedProduct createOnboardedProduct(String roleId, String productId, OnboardedProductState status,
                                                    PartyRole role, String productRole, Boolean toAddOnAggregates) {
        OnboardedProduct product = new OnboardedProduct();
        product.setRoleId(roleId);
        product.setProductId(productId);
        product.setStatus(status);
        product.setRole(role);
        product.setProductRole(productRole);
        product.setToAddOnAggregates(toAddOnAggregates);
        return product;
    }

    private UserInstitution createUserInstitution(String institutionId, String userId, List<OnboardedProduct> products, String userMailUuid) {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setInstitutionId(institutionId);
        userInstitution.setUserId(userId);
        userInstitution.setProducts(products);
        userInstitution.setUserMailUuid(userMailUuid);
        return userInstitution;
    }

}
