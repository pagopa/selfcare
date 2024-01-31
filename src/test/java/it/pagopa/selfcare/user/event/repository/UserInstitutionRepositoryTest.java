package it.pagopa.selfcare.user.event.repository;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.constant.OnboardedProductState;
import it.pagopa.selfcare.user.event.entity.OnboardedProduct;
import it.pagopa.selfcare.user.event.entity.UserInfo;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.event.repository.UserInstitutionRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class UserInstitutionRepositoryTest {

    @Inject
    UserInstitutionRepository userInstitutionRepository;

    final static UserInstitution userResource;

    static {
        userResource = new UserInstitution();
        userResource.setInstitutionId("institutionId");
        userResource.setUserId("userId");
        userResource.setProducts(Collections.emptyList());
    }

    void mockPersistUserInfo(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(UserInfo.class));
        asserter.execute(() -> when(UserInfo.persist(any(UserInfo.class), any()))
                .thenAnswer(arg -> Uni.createFrom().nullItem()));
    }

    void mockRetrieveUserInfoFounded(UniAsserter asserter) {
        asserter.execute(() -> PanacheMock.mock(UserInfo.class));
        asserter.execute(() -> when(UserInfo.findByIdOptional(anyString()))
                .thenAnswer(arg -> {
                    UserInfo userInfo = (UserInfo) arg.getArguments()[0];
                    userInfo.setUserId("userId");
                    UserInstitutionRole role = new UserInstitutionRole();
                    role.setInstitutionId("institutionId");
                    role.setInstitutionName("institutionName");
                    role.setRole(PartyRole.DELEGATE);
                    userInfo.setInstitutions(List.of(role));
                    return Uni.createFrom().item(Optional.of(userInfo));
                }));
    }

    @Test
    @RunOnVertxContext
    void initOrderStreamWithFoundedUserIdAndInstitutionId(UniAsserter asserter){
        UserInstitution userInstitution = constructUserInstitution();
        mockRetrieveUserInfoFounded(asserter);
        mockPersistUserInfo(asserter);
        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(retrieveUserInfo())));
        UniAssertSubscriber<Void> subscriber =  userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }

    @Test
    @RunOnVertxContext
    void initOrderStreamWithFoundedUserId(UniAsserter asserter){
        UserInstitution userInstitution = constructUserInstitution();
        userInstitution.setInstitutionId("institutionId2");
        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(retrieveUserInfo())));
        mockPersistUserInfo(asserter);
        UniAssertSubscriber<Void> subscriber =  userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }

    @Test
    @RunOnVertxContext
    void initOrderStreamWithNotFoundUserId(UniAsserter asserter){
        UserInstitution userInstitution = constructUserInstitution();
        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findByIdOptional(anyString()))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        UniAssertSubscriber<Void> subscriber =  userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }

    @Test
    @RunOnVertxContext
    void initOrderStreamWithNotFoundValidState(UniAsserter asserter){
        UserInstitution userInstitution = constructUserInstitution();
        userInstitution.getProducts().get(0).setStatus(OnboardedProductState.SUSPENDED);
        PanacheMock.mock(UserInfo.class);
        UserInfo userInfo = retrieveUserInfo();
        List<UserInstitutionRole> userInstitutionRoles = new ArrayList<>(userInfo.getInstitutions());
        userInstitutionRoles.removeIf(userInstitutionRole -> userInstitutionRole.getInstitutionId().equalsIgnoreCase("institutionId3"));
        userInfo.setInstitutions(userInstitutionRoles);
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(userInfo)));
        mockPersistUserInfo(asserter);
        UniAssertSubscriber<Void> subscriber =  userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();    }

    @Test
    @RunOnVertxContext
    void initOrderStreamWithNotFoundValidStateAndInstitutionId(UniAsserter asserter){
        UserInstitution userInstitution = constructUserInstitution();
        userInstitution.setInstitutionId("institutionId2");
        userInstitution.getProducts().get(0).setStatus(OnboardedProductState.SUSPENDED);
        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(retrieveUserInfo())));
        mockPersistUserInfo(asserter);
        UniAssertSubscriber<Void> subscriber =  userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();    }


    @Test
    @RunOnVertxContext
    void initOrderStreamWithNotFoundValidStateButMultipleInstitutionInUserInfo(UniAsserter asserter){
        UserInstitution userInstitution = constructUserInstitution();
        userInstitution.setInstitutionId("institutionId2");
        userInstitution.getProducts().get(0).setStatus(OnboardedProductState.SUSPENDED);
        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(retrieveUserInfo())));
        mockPersistUserInfo(asserter);
        UniAssertSubscriber<Void> subscriber =  userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();    }


    @Test
    @RunOnVertxContext
    void initOrderStreamWithNotFoundValidStateButMoreInstitutions(UniAsserter asserter){
        UserInstitution userInstitution = constructUserInstitution();
        userInstitution.getProducts().get(0).setStatus(OnboardedProductState.SUSPENDED);
        PanacheMock.mock(UserInfo.class);
        UserInfo userInfo = retrieveUserInfo();
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(userInfo)));
        mockPersistUserInfo(asserter);
        UniAssertSubscriber<Void> subscriber =  userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();    }

    private UserInfo retrieveUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("userId");
        UserInstitutionRole role = new UserInstitutionRole();
        role.setInstitutionId("institutionId");
        role.setInstitutionName("institutionName");
        role.setRole(PartyRole.DELEGATE);
        UserInstitutionRole role1 = new UserInstitutionRole();
        role1.setInstitutionId("institutionId3");
        role1.setInstitutionName("institutionName");
        role1.setRole(PartyRole.DELEGATE);
        userInfo.setInstitutions(List.of(role, role1));
        return userInfo;
    }

    private UserInstitution constructUserInstitution() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId("institutionId");
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setProductId("productId");
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
        userInstitution.setProducts(List.of(onboardedProduct));
        return userInstitution;
    }



}
