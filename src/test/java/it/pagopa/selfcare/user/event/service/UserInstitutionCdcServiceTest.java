package it.pagopa.selfcare.user.event.service;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.client.EventHubFdRestClient;
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.event.UserInstitutionCdcService;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.repository.UserInstitutionRepository;
import it.pagopa.selfcare.user.model.FdUserNotificationToSend;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.inject.Inject;
import org.bson.BsonDocument;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
    EventHubRestClient eventHubRestClient;

    @RestClient
    @InjectMock
    EventHubFdRestClient eventHubFdRestClient;

    @InjectMock
    UserInstitutionRepository userInstitutionRepository;


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
}
