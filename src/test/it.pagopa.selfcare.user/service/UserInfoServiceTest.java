package service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.service.UserInfoServiceDefault;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class UserInfoServiceTest {

    @Inject
    private UserInfoServiceDefault userInfoService;

    @Test
    void findById() {
        final String id = new ObjectId().toHexString();
        UserInfo userInfo = createDummyUserInfo();
        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findById(any()))
                .thenReturn(Uni.createFrom().item(userInfo));
        Uni<UserInfoResponse> response = userInfoService.findById(id);
        Assertions.assertNotNull(response);
    }

    @Test
    void findByUserId() {
        final String userId = "userId";
        UserInfo userInfo = createDummyUserInfo();
        PanacheMock.mock(UserInfo.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInfo));
        when(UserInfo.find(any(), (Object) any()))
                .thenReturn(query);
        Uni<UserInfoResponse> response = userInfoService.findByUserId(userId);
        Assertions.assertNotNull(response);
    }

    private UserInfo createDummyUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(ObjectId.get());
        userInfo.setUserId("userId");
        return userInfo;
    }

}
