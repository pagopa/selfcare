package it.pagopa.selfcare.user.event.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class UserInstitutionRepositoryTest {

    public static final String INSTITUTION_ID = "institutionId";
    @Inject
    UserInstitutionRepository userInstitutionRepository;

    final static UserInstitution userResource;

    static {
        userResource = new UserInstitution();
        userResource.setInstitutionId("institutionId");
        userResource.setUserId("userId");
        userResource.setProducts(Collections.emptyList());
    }

    private OnboardedProduct dummyOnboardedProduct(String productId, PartyRole partyRole) {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setRole(partyRole);
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
        return onboardedProduct;
    }

    void mockPersistUserInstitution(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(UserInstitution.class));
        asserter.execute(() -> when(UserInstitution.persist(any(UserInstitution.class), any()))
                .thenAnswer(arg -> Uni.createFrom().nullItem()));
    }

    @Test
    @RunOnVertxContext
    public void propagateUserToAggregateTestWithoutExistingUserInstitution(UniAsserter asserter) {
        final OnboardedProduct parentProduct = dummyOnboardedProduct("parentProduct", PartyRole.DELEGATE);
        parentProduct.setRoleId("roleId");
        parentProduct.setToAddOnAggregates(true);
        final UserInstitution parentUser = new UserInstitution();
        parentUser.setUserId("userId");
        parentUser.setUserMailUuid("userMailUuid");
        parentUser.setInstitutionDescription("parentInstitutionDescription");
        parentUser.setProducts(List.of(
                dummyOnboardedProduct("dummy1", PartyRole.OPERATOR),
                parentProduct,
                dummyOnboardedProduct("dummy2", PartyRole.MANAGER)
        ));
        final String aggregateId = "aggregateId";
        final String aggregateDescription = "aggregateDescription";
        final PartyRole roleToPropagate = PartyRole.ADMIN_EA;
        final String productRoleToPropagate = "admin";

        PanacheMock.mock(UserInstitution.class);
        final ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        when(UserInstitution.find(anyString(), any(Object[].class))).thenReturn(query);
        when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.empty()));
        mockPersistUserInstitution(asserter);

        userInstitutionRepository.propagateUserToAggregate(parentUser, parentProduct, aggregateId, aggregateDescription,
                roleToPropagate, productRoleToPropagate).subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        asserter.execute(() -> {
            final ArgumentCaptor<UserInstitution> argumentCaptor = ArgumentCaptor.forClass(UserInstitution.class);
            PanacheMock.verify(UserInstitution.class, Mockito.atLeastOnce()).persist(argumentCaptor.capture(), any());
            assertEquals(parentUser.getUserId(), argumentCaptor.getValue().getUserId());
            assertEquals(aggregateId, argumentCaptor.getValue().getInstitutionId());
            assertEquals(aggregateDescription, argumentCaptor.getValue().getInstitutionDescription());
            assertEquals(parentUser.getUserMailUuid(), argumentCaptor.getValue().getUserMailUuid());
            assertEquals(parentUser.getInstitutionDescription(), argumentCaptor.getValue().getInstitutionRootName());
            assertEquals(1, argumentCaptor.getValue().getProducts().size());
            assertEquals(parentProduct.getRoleId(), argumentCaptor.getValue().getProducts().get(0).getRoleId());
            assertEquals(parentProduct.getProductId(), argumentCaptor.getValue().getProducts().get(0).getProductId());
            assertEquals(parentProduct.getStatus(), argumentCaptor.getValue().getProducts().get(0).getStatus());
            assertEquals(roleToPropagate, argumentCaptor.getValue().getProducts().get(0).getRole());
            assertEquals(productRoleToPropagate, argumentCaptor.getValue().getProducts().get(0).getProductRole());
            assertNull(argumentCaptor.getValue().getProducts().get(0).getToAddOnAggregates());
        });
    }

    @Test
    @RunOnVertxContext
    public void propagateUserToAggregateTestWithExistingUserInstitution(UniAsserter asserter) {
        final OnboardedProduct parentProduct = dummyOnboardedProduct("parentProduct", PartyRole.DELEGATE);
        parentProduct.setRoleId("roleId");
        parentProduct.setToAddOnAggregates(true);
        final UserInstitution parentUser = new UserInstitution();
        parentUser.setUserId("userId");
        parentUser.setUserMailUuid("userMailUuid");
        parentUser.setInstitutionDescription("parentInstitutionDescription");
        parentUser.setProducts(List.of(
                dummyOnboardedProduct("dummy1", PartyRole.OPERATOR),
                parentProduct,
                dummyOnboardedProduct("dummy2", PartyRole.MANAGER)
        ));
        final String aggregateId = "aggregateId";
        final String aggregateDescription = "aggregateDescription";
        final PartyRole roleToPropagate = PartyRole.ADMIN_EA;
        final String productRoleToPropagate = "admin";

        final UserInstitution existingUserInstitution = new UserInstitution();
        existingUserInstitution.setId(new ObjectId());
        existingUserInstitution.setUserId(parentUser.getUserId());
        existingUserInstitution.setInstitutionId(aggregateId);
        existingUserInstitution.setInstitutionDescription("oldDescription");
        existingUserInstitution.setUserMailUuid("oldMailUuid");
        existingUserInstitution.setInstitutionRootName("oldRootName");
        existingUserInstitution.setProducts(List.of(dummyOnboardedProduct("otherProduct", PartyRole.OPERATOR)));

        PanacheMock.mock(UserInstitution.class);
        final ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        when(UserInstitution.find(anyString(), any(Object[].class))).thenReturn(query);
        when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(existingUserInstitution)));

        userInstitutionRepository.propagateUserToAggregate(parentUser, parentProduct, aggregateId, aggregateDescription,
                roleToPropagate, productRoleToPropagate).subscribe().withSubscriber(UniAssertSubscriber.create());

        asserter.execute(() -> {
            assertEquals(parentUser.getUserId(), existingUserInstitution.getUserId());
            assertEquals(aggregateId, existingUserInstitution.getInstitutionId());
            assertEquals(aggregateDescription, existingUserInstitution.getInstitutionDescription());
            assertEquals(parentUser.getUserMailUuid(), existingUserInstitution.getUserMailUuid());
            assertEquals(parentUser.getInstitutionDescription(), existingUserInstitution.getInstitutionRootName());
            assertEquals(2, existingUserInstitution.getProducts().size());
            assertEquals("otherProduct", existingUserInstitution.getProducts().get(0).getProductId());
            assertEquals(PartyRole.OPERATOR, existingUserInstitution.getProducts().get(0).getRole());
            assertEquals(OnboardedProductState.ACTIVE, existingUserInstitution.getProducts().get(0).getStatus());
            assertEquals(parentProduct.getRoleId(), existingUserInstitution.getProducts().get(1).getRoleId());
            assertEquals(parentProduct.getProductId(), existingUserInstitution.getProducts().get(1).getProductId());
            assertEquals(parentProduct.getStatus(), existingUserInstitution.getProducts().get(1).getStatus());
            assertEquals(roleToPropagate, existingUserInstitution.getProducts().get(1).getRole());
            assertEquals(productRoleToPropagate, existingUserInstitution.getProducts().get(1).getProductRole());
            assertNull(existingUserInstitution.getProducts().get(1).getToAddOnAggregates());
        });
    }

    @Test
    @RunOnVertxContext
    public void propagateUserToAggregateTestWithExistingUserInstitutionAndProduct(UniAsserter asserter) {
        final OnboardedProduct parentProduct = dummyOnboardedProduct("parentProduct", PartyRole.DELEGATE);
        parentProduct.setRoleId("roleId");
        parentProduct.setToAddOnAggregates(true);
        final UserInstitution parentUser = new UserInstitution();
        parentUser.setUserId("userId");
        parentUser.setUserMailUuid("userMailUuid");
        parentUser.setInstitutionDescription("parentInstitutionDescription");
        parentUser.setProducts(List.of(
                dummyOnboardedProduct("dummy1", PartyRole.OPERATOR),
                parentProduct,
                dummyOnboardedProduct("dummy2", PartyRole.MANAGER)
        ));
        final String aggregateId = "aggregateId";
        final String aggregateDescription = "aggregateDescription";
        final PartyRole roleToPropagate = PartyRole.ADMIN_EA;
        final String productRoleToPropagate = "admin";

        final OnboardedProduct existingProduct = dummyOnboardedProduct("existingProduct", PartyRole.OPERATOR);
        existingProduct.setRoleId("roleId");

        final UserInstitution existingUserInstitution = new UserInstitution();
        existingUserInstitution.setId(new ObjectId());
        existingUserInstitution.setUserId(parentUser.getUserId());
        existingUserInstitution.setInstitutionId(aggregateId);
        existingUserInstitution.setInstitutionDescription("oldDescription");
        existingUserInstitution.setUserMailUuid("oldMailUuid");
        existingUserInstitution.setInstitutionRootName("oldRootName");
        existingUserInstitution.setProducts(List.of(
                dummyOnboardedProduct("otherProduct", PartyRole.OPERATOR),
                existingProduct
        ));

        PanacheMock.mock(UserInstitution.class);
        final ReactivePanacheQuery query = mock(ReactivePanacheQuery.class);
        when(UserInstitution.find(anyString(), any(Object[].class))).thenReturn(query);
        when(query.firstResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(existingUserInstitution)));

        userInstitutionRepository.propagateUserToAggregate(parentUser, parentProduct, aggregateId, aggregateDescription,
                roleToPropagate, productRoleToPropagate).subscribe().withSubscriber(UniAssertSubscriber.create());

        asserter.execute(() -> {
            assertEquals(parentUser.getUserId(), existingUserInstitution.getUserId());
            assertEquals(aggregateId, existingUserInstitution.getInstitutionId());
            assertEquals(aggregateDescription, existingUserInstitution.getInstitutionDescription());
            assertEquals(parentUser.getUserMailUuid(), existingUserInstitution.getUserMailUuid());
            assertEquals(parentUser.getInstitutionDescription(), existingUserInstitution.getInstitutionRootName());
            assertEquals(2, existingUserInstitution.getProducts().size());
            assertEquals("otherProduct", existingUserInstitution.getProducts().get(0).getProductId());
            assertEquals(PartyRole.OPERATOR, existingUserInstitution.getProducts().get(0).getRole());
            assertEquals(OnboardedProductState.ACTIVE, existingUserInstitution.getProducts().get(0).getStatus());
            assertEquals(parentProduct.getRoleId(), existingUserInstitution.getProducts().get(1).getRoleId());
            assertEquals(parentProduct.getProductId(), existingUserInstitution.getProducts().get(1).getProductId());
            assertEquals(parentProduct.getStatus(), existingUserInstitution.getProducts().get(1).getStatus());
            assertEquals(roleToPropagate, existingUserInstitution.getProducts().get(1).getRole());
            assertEquals(productRoleToPropagate, existingUserInstitution.getProducts().get(1).getProductRole());
            assertNull(existingUserInstitution.getProducts().get(1).getToAddOnAggregates());
        });
    }

}
