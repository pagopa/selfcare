package it.pagopa.selfcare.user.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class UserRegistryServiceTest {
    @Inject
    @Any
    InMemoryConnector connector;
    @Inject
    UserRegistryService userRegistryService;

    @InjectMock
    private UserInstitutionService userInstitutionService;

    @RestClient
    @InjectMock
    private org.openapi.quarkus.user_registry_json.api.UserApi userRegistryApi;

    private static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";
    private static org.openapi.quarkus.user_registry_json.model.UserResource userResource;
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
        certifiedEmail.setValue("test@test.it");
        org.openapi.quarkus.user_registry_json.model.WorkContactResource workContactResource = new org.openapi.quarkus.user_registry_json.model.WorkContactResource();
        workContactResource.setEmail(certifiedEmail);
        userResource.setEmail(certifiedEmail);
        userResource.setWorkContacts(Map.of("institutionId", workContactResource));

        userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId("institutionId");
        OnboardedProduct product = new OnboardedProduct();
        product.setProductId("test");
        userInstitution.setProducts(List.of(product));
    }


    @BeforeAll
    public static void switchMyChannels() {
        InMemoryConnector.switchOutgoingChannelsToInMemory("sc-users");
    }

    @Test
    void testSendUpdateUserNotificationToQueue() {
        MutableUserFieldsDto mutableUserFieldsDto = new MutableUserFieldsDto();
        InMemorySink<String> usersOut = connector.sink("sc-users");
        when(userInstitutionService.findAllWithFilter(anyMap())).thenReturn(Multi.createFrom().item(userInstitution));
        when(userRegistryApi.updateUsingPATCH("userId", mutableUserFieldsDto)).thenReturn(Uni.createFrom().item(Response.accepted().build()));
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, "userId")).thenReturn(Uni.createFrom().item(userResource));
        UniAssertSubscriber<Void> subscriber = userRegistryService.updateUserRegistryAndSendNotificationToQueue(mutableUserFieldsDto, "userId", "institutionId")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();

        // Wait that the event is sent on kafka.
        await().<List<? extends Message<String>>>until(usersOut::received, t -> t.size() == 1);

        String queuedMessage = usersOut.received().get(0).getPayload();
        Assertions.assertTrue(queuedMessage.contains("userId"));
    }


}
