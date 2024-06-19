package it.pagopa.selfcare.user.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UpdateUserRequest;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.user_registry_json.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class UserRegistryServiceTest {
    @Inject
    UserRegistryService userRegistryService;

    @InjectMock
    private UserInstitutionService userInstitutionService;

    @InjectMock
    private UserNotificationService userNotificationService;

    @RestClient
    @InjectMock
    private org.openapi.quarkus.user_registry_json.api.UserApi userRegistryApi;

    private static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";
    private static final org.openapi.quarkus.user_registry_json.model.UserResource userResource;
    private static final String userMailDefault = "test@test.it";
    private static final String userMailUuidDefault = "ID_MAIL#123455";
    private static UserInstitution userInstitution;

    static {
        userResource = new org.openapi.quarkus.user_registry_json.model.UserResource();
        userResource.setId(UUID.randomUUID());
        org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring certifiedName = new org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring();
        certifiedName.setValue("name");
        userResource.setName(certifiedName);
        userResource.setFamilyName(certifiedName);
        userResource.setFiscalCode("taxCode");
        org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring certifiedEmail = new org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring();
        certifiedEmail.setValue(userMailDefault);
        org.openapi.quarkus.user_registry_json.model.WorkContactResource workContactResource = new org.openapi.quarkus.user_registry_json.model.WorkContactResource();
        workContactResource.setEmail(certifiedEmail);
        userResource.setEmail(certifiedEmail);
        userResource.setWorkContacts(Map.of(userMailUuidDefault, workContactResource));

        userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId("institutionId");
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        userInstitution.setProducts(List.of(product));
    }

    @Test
    public void shouldReturnUserResourceWhenFindByIdUsingGETIsCalled() {
        when(userRegistryApi.findByIdUsingGET("fl", "id")).thenReturn(Uni.createFrom().item(new UserResource()));
        UniAssertSubscriber<UserResource> subscriber = userRegistryService.findByIdUsingGET("fl", "id")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
        verify(userRegistryApi, times(1)).findByIdUsingGET("fl", "id");
    }

    @Test
    public void shouldReturnUserIdWhenSaveUsingPATCHIsCalled() {
        when(userRegistryApi.saveUsingPATCH(any(SaveUserDto.class))).thenReturn(Uni.createFrom().item(new UserId()));
        UniAssertSubscriber<UserId> subscriber = userRegistryService.saveUsingPATCH(new SaveUserDto())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
        verify(userRegistryApi, times(1)).saveUsingPATCH(any(SaveUserDto.class));
    }

    @Test
    public void shouldReturnUserResourceWhenSearchUsingPOSTIsCalled() {
        UserSearchDto userSearchDto = new UserSearchDto();
        when(userRegistryApi.searchUsingPOST("workContact", userSearchDto)).thenReturn(Uni.createFrom().item(new UserResource()));
        UniAssertSubscriber<UserResource> subscriber = userRegistryService.searchUsingPOST("workContact", userSearchDto)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
        verify(userRegistryApi, times(1)).searchUsingPOST("workContact", userSearchDto);
    }

    @Test
    public void shouldReturnResponseWhenUpdateUsingPATCHIsCalled() {
        MutableUserFieldsDto mutableUserFieldsDto = new MutableUserFieldsDto();
        when(userRegistryApi.updateUsingPATCH("id", mutableUserFieldsDto)).thenReturn(Uni.createFrom().item(Response.accepted().build()));
        UniAssertSubscriber<Response> subscriber = userRegistryService.updateUsingPATCH("id", mutableUserFieldsDto)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
        verify(userRegistryApi, times(1)).updateUsingPATCH("id", mutableUserFieldsDto);
    }

    @BeforeAll
    public static void switchMyChannels() {
        InMemoryConnector.switchOutgoingChannelsToInMemory("sc-users");
    }

    @Test
    void updateUserRegistryAndSendNotificationToQueue_shouldThrowExceptionWhenMailIsNull() {
        assertThrows(IllegalArgumentException.class, () -> userRegistryService.updateUserRegistry(new UpdateUserRequest(), null, null));
    }

    @Test
    void updateUserRegistryAndSendNotificationToQueue_whenUserRegistryNothingToUpdate() {
        final String userId = userResource.getId().toString();
        final String institutionId = "institutionId";
        when(userNotificationService.sendKafkaNotification(any(UserNotificationToSend.class), anyString())).thenReturn(Uni.createFrom().item(new UserNotificationToSend()));

        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setName(userResource.getName().getValue());
        updateUserRequest.setEmail(userMailDefault);

        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(userInstitution));
        when(userInstitutionService.persistOrUpdate(any(UserInstitution.class))).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(eq(userId), any(MutableUserFieldsDto.class))).thenReturn(Uni.createFrom().item(Response.accepted().build()));
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userId)).thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<List<UserInstitution>> subscriber = userRegistryService.updateUserRegistry(updateUserRequest, userId, institutionId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();

        ArgumentCaptor<MutableUserFieldsDto> userFieldsDtoArgumentCaptor = ArgumentCaptor.forClass(MutableUserFieldsDto.class);
        verify(userRegistryApi, times(1))
                .updateUsingPATCH(eq(userId), userFieldsDtoArgumentCaptor.capture());
        assertNull(userFieldsDtoArgumentCaptor.getValue().getName());

        ArgumentCaptor<UserInstitution> userInstitutionArgumentCaptor = ArgumentCaptor.forClass(UserInstitution.class);
        verify(userInstitutionService, times(1))
                .persistOrUpdate(userInstitutionArgumentCaptor.capture());
        assertEquals(userMailUuidDefault, userInstitutionArgumentCaptor.getValue().getUserMailUuid());
    }

    @Test
    void updateUserRegistryAndSendNotificationToQueue_whenUserRegistryMustUpdate() {
        final String userId = userResource.getId().toString();
        final String institutionId = "institutionId";
        when(userNotificationService.sendKafkaNotification(any(UserNotificationToSend.class), anyString())).thenReturn(Uni.createFrom().item(new UserNotificationToSend()));

        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setName("example");
        updateUserRequest.setEmail("example@example.it");

        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(userInstitution));
        when(userInstitutionService.persistOrUpdate(any(UserInstitution.class))).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(eq(userId), any(MutableUserFieldsDto.class))).thenReturn(Uni.createFrom().item(Response.accepted().build()));
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userId)).thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<List<UserInstitution>> subscriber = userRegistryService.updateUserRegistry(updateUserRequest, userId, institutionId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();

        //User should be updated for name and mail with a new MailUuid
        ArgumentCaptor<MutableUserFieldsDto> userFieldsDtoArgumentCaptor = ArgumentCaptor.forClass(MutableUserFieldsDto.class);
        verify(userRegistryApi, times(1))
                .updateUsingPATCH(eq(userId), userFieldsDtoArgumentCaptor.capture());
        assertEquals(updateUserRequest.getName(), userFieldsDtoArgumentCaptor.getValue().getName().getValue());
        assertNotNull(userFieldsDtoArgumentCaptor.getValue().getWorkContacts());
        final String userMailUuid = userFieldsDtoArgumentCaptor.getValue().getWorkContacts().keySet().stream().findFirst().orElse(null);
        assertNotNull(userMailUuid);
        assertEquals(updateUserRequest.getEmail(), userFieldsDtoArgumentCaptor.getValue().getWorkContacts().get(userMailUuid).getEmail().getValue());

        //UserInstitution should update a new MailUuid
        ArgumentCaptor<UserInstitution> userInstitutionArgumentCaptor = ArgumentCaptor.forClass(UserInstitution.class);
        verify(userInstitutionService, times(1))
                .persistOrUpdate(userInstitutionArgumentCaptor.capture());
        assertEquals(userMailUuid, userInstitutionArgumentCaptor.getValue().getUserMailUuid());
    }

    @Test
    void testSendUpdateUserNotificationToQueue() {
        final String userId = userResource.getId().toString();
        final String institutionId = "institutionId";
        when(userNotificationService.sendKafkaNotification(any(UserNotificationToSend.class), anyString())).thenReturn(Uni.createFrom().item(new UserNotificationToSend()));

        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("test2@test.it");
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(userInstitution));
        when(userInstitutionService.persistOrUpdate(any(UserInstitution.class))).thenReturn(Uni.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(eq(userId), any(MutableUserFieldsDto.class))).thenReturn(Uni.createFrom().item(Response.accepted().build()));
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userId)).thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<List<UserInstitution>> subscriber = userRegistryService.updateUserRegistry(updateUserRequest, userId, institutionId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }

    @Test
    void testSendUpdateUserNotificationToQueue2() {
        when(userNotificationService.sendKafkaNotification(any(UserNotificationToSend.class), anyString())).thenReturn(Uni.createFrom().item(new UserNotificationToSend()));

        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("test@test.it");
        when(userInstitutionService.persistOrUpdate(any(UserInstitution.class))).thenReturn(Uni.createFrom().item(userInstitution));
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH(eq("userId"), any(MutableUserFieldsDto.class))).thenReturn(Uni.createFrom().item(Response.accepted().build()));
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, "userId")).thenReturn(Uni.createFrom().item(userResource));
        UniAssertSubscriber<List<UserInstitution>> subscriber = userRegistryService.updateUserRegistry(updateUserRequest, "userId", "institutionId")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }
}
