package it.pagopa.selfcare.user.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class UserServiceTest {

    @Test
    void getUsersEmailByInstitutionTest() {
        UserResource userResource = createDummyUserResource();
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().item(userInstitution));
        when(UserInstitution.find(any(), (Object) any()))
                .thenReturn(query);
        when(userRegistryApi.findByIdUsingGET(any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<List<String>> subscriber = userInstitutionService
                .getUsersEmailByInstitution("institutionId")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        List<String> actual = subscriber.assertCompleted().awaitItem().getItem();
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("test@test.it", actual.get(0));
    }

    private UserResource createDummyUserResource() {
        var userResource = new UserResource();
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
        userResource.setWorkContacts(Map.of("institutionId", workContactResource));
        return userResource;
    }

}
