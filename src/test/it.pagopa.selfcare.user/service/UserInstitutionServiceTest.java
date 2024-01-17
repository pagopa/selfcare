package service;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class UserInstitutionServiceTest {

    @Inject
    private UserInstitutionServiceDefault userInstitutionService;

    @Test
    @RunOnVertxContext
    void findById(UniAsserter asserter) {
        final String id = new ObjectId().toHexString();
        asserter.execute(() -> PanacheMock.mock(UserInstitution.class));
        asserter.execute(() -> when(UserInstitution.findById(anyString()))
                .thenAnswer(arg -> {
                    UserInstitution userInstitution = (UserInstitution) arg.getArguments()[0];
                    userInstitution.setId(ObjectId.get());
                    return Uni.createFrom().nullItem();
                }));
        Uni<UserInstitutionResponse> response = userInstitutionService.findById(id);
        Assertions.assertNotNull(response);
    }

    @Test
    @RunOnVertxContext
    void findByInstitutionId(UniAsserter asserter) {
        final String institutionId = "institutionId";
        mockfindUserInstitution(asserter);
        Uni<UserInstitutionResponse> response = userInstitutionService.findByInstitutionId(institutionId);
        Assertions.assertNotNull(response);
    }

    @Test
    @RunOnVertxContext
    void findByUserId(UniAsserter asserter) {
        final String userId = "userId";
        mockfindUserInstitution(asserter);
        Multi<UserInstitutionResponse> response = userInstitutionService.findByUserId(userId);
        Assertions.assertNotNull(response);
    }

    void mockfindUserInstitution(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(UserInstitution.class));
        asserter.execute(() -> when(UserInstitution.find(anyString(), anyString()))
                .thenAnswer(arg -> {
                    UserInstitution userInstitution = (UserInstitution) arg.getArguments()[0];
                    userInstitution.setId(ObjectId.get());
                    return Uni.createFrom().nullItem();
                }));
    }

}
