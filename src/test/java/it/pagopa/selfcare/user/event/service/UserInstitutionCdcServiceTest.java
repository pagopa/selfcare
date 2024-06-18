package it.pagopa.selfcare.user.event.service;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.event.UserInstitutionCdcService;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.inject.Inject;
import org.bson.BsonDocument;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.time.LocalDateTime;
import java.util.List;

import static it.pagopa.selfcare.user.event.UserInstitutionCdcService.USERS_FIELD_LIST_WITHOUT_FISCAL_CODE;
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

    @Test
    void consumerToSendScUserEvent() {
        UserInstitution userInstitution = dummyUserInstitution();
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
                sendMessage(any());
    }

    UserResource dummyUserResource() {
        UserResource userResource = new UserResource();

        return userResource;
    }

    UserInstitution dummyUserInstitution() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setProducts(List.of(dummyOnboardedProduct("example-1", OnboardedProductState.ACTIVE, 1),
                dummyOnboardedProduct("example-2", OnboardedProductState.DELETED, 1)));
        return userInstitution;

    }

    OnboardedProduct dummyOnboardedProduct(String productRole, OnboardedProductState state, int day) {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId("productId");
        onboardedProduct.setProductRole(productRole);
        onboardedProduct.setCreatedAt(LocalDateTime.of(2024,1,day,0,0,0));
        onboardedProduct.setUpdatedAt(LocalDateTime.of(2024,1,day,0,0,0));
        onboardedProduct.setStatus(state);
        return onboardedProduct;
    }

}
