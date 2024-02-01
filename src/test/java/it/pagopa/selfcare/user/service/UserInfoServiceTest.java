package it.pagopa.selfcare.user.service;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class UserInfoServiceTest {

    @Inject
    private UserInfoServiceDefault userInfoService;


    @Test
    void findByUserId() {
        final String userId = "userId";
        UserInfo userInfo = createDummyUserInfo();
        PanacheMock.mock(UserInfo.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInfo));
        when(UserInfo.find(any(), (Object) any()))
                .thenReturn(query);
        Uni<UserInfoResponse> response = userInfoService.findById(userId);
        Assertions.assertNotNull(response);
    }

    private UserInfo createDummyUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("userId");
        return userInfo;
    }

}
