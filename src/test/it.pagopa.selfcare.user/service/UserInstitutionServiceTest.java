package service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.service.UserInstitutionServiceDefault;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class UserInstitutionServiceTest {

    @Inject
    private UserInstitutionServiceDefault userInstitutionService;

    @Test
    void findById() {
        final String id = new ObjectId().toHexString();
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        when(UserInstitution.findById(any()))
                .thenReturn(Uni.createFrom().item(userInstitution));
        Uni<UserInstitutionResponse> response = userInstitutionService.findById(id);
        Assertions.assertNotNull(response);
    }

    @Test
    void findByInstitutionId() {
        final String institutionId = "institutionId";
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find(any(), (Object) any()))
                .thenReturn(query);
        Uni<UserInstitutionResponse> response = userInstitutionService.findByInstitutionId(institutionId);
        Assertions.assertNotNull(response);
    }

    @Test
    void findByUserId(UniAsserter asserter) {
        final String userId = "userId";
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find(any(), (Object) any()))
                .thenReturn(query);
        Multi<UserInstitutionResponse> response = userInstitutionService.findByUserId(userId);
        Assertions.assertNotNull(response);
    }

    private UserInstitution createDummyUserInstitution() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId("institutionId");
        return userInstitution;
    }


}
