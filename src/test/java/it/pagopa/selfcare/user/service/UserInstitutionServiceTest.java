package it.pagopa.selfcare.user.service;

import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter.OnboardedProductEnum.PRODUCT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter.UserInstitutionFilterEnum.INSTITUTION_ID;
import static it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter.UserInstitutionFilterEnum.USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class UserInstitutionServiceTest {

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
    void findByUserId() {
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

    @Test
    void paginatedFindAllWithFilter() {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("institutionId", "institutionId");
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find((Document) any(), any()))
                .thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.list()).thenReturn(Uni.createFrom().item(List.of(userInstitution)));
        UniAssertSubscriber<List<UserInstitution>> subscriber =  userInstitutionService.paginatedFindAllWithFilter(parameterMap, 0, 1)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(List.of(userInstitution));
    }


    @Test
    void retrieveFirstFilteredUserInstitution() {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("institutionId", "institutionId");
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find((Document) any(), any()))
                .thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        UniAssertSubscriber<UserInstitution> subscriber =  userInstitutionService.retrieveFirstFilteredUserInstitution(parameterMap)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(userInstitution);
    }

    @Test
    void findAllWithFilter() {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("institutionId", "institutionId");
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find((Document) any(), any()))
                .thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(userInstitution));

        AssertSubscriber<UserInstitution> subscriber =  userInstitutionService.findAllWithFilter(parameterMap)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        List<UserInstitution> actual = subscriber.assertCompleted().getItems();
        assertNotNull(actual);
        assertEquals(1, actual.size());
    }

    @Test
    void updateUserStatusWithInstitutionAndOnboardedFilter() {
        final String userId = "userId";
        String institutionId = "institutionId";
        String productId = "productId";
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update((Document) any()))
                .thenReturn(update);
        when(update.where(any())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(userId, institutionId, productId, null, null, OnboardedProductState.ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void updateUserStatusWithInstitutionFilter() {
        final String userId = "userId";
        String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update((Document) any()))
                .thenReturn(update);
        when(update.where(any())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(userId, institutionId, null, null, null, OnboardedProductState.ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void deleteUserInstitutionProduct(){
        final String userId = "userId";
        String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update(any(Document.class)))
                .thenReturn(update);
        when(update.where(any())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.deleteUserInstitutionProduct(userId, institutionId, "productID")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    private UserInstitution createDummyUserInstitution() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId("institutionId");
        return userInstitution;
    }


}
