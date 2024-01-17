package service;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.service.UserInfoServiceDefault;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class UserInfoServiceTest {

    @Inject
    private UserInfoServiceDefault userInfoService;

    @Test
    @RunOnVertxContext
    void findById(UniAsserter asserter) {
        final String id = new ObjectId().toHexString();
        asserter.execute(() -> PanacheMock.mock(UserInfo.class));
        asserter.execute(() -> when(UserInfo.findById(anyString()))
                .thenAnswer(arg -> {
                    UserInfo userInfo = (UserInfo) arg.getArguments()[0];
                    userInfo.setId(ObjectId.get());
                    return Uni.createFrom().nullItem();
                }));
        Uni<UserInfoResponse> response = userInfoService.findById(id);
        Assertions.assertNotNull(response);
    }

    @Test
    @RunOnVertxContext
    void findByUserId(UniAsserter asserter) {
        final String userId = "userId";
        mockfindUserInfo(asserter);
        Uni<UserInfoResponse> response = userInfoService.findByUserId(userId);
        Assertions.assertNotNull(response);
    }

    void mockfindUserInfo(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(UserInfo.class));
        asserter.execute(() -> when(UserInfo.find(anyString(), anyString()))
                .thenAnswer(arg -> {
                    UserInfo userInfo = (UserInfo) arg.getArguments()[0];
                    userInfo.setId(ObjectId.get());
                    return Uni.createFrom().nullItem();
                }));
    }

}
