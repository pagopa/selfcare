package it.pagopa.selfcare.user.event.repository;

import com.azure.data.tables.TableClient;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.UserInstitutionCdcService;
import it.pagopa.selfcare.user.event.constant.OnboardedProductState;
import it.pagopa.selfcare.user.event.entity.OnboardedProduct;
import it.pagopa.selfcare.user.event.entity.UserInfo;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.event.repository.UserInstitutionRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
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
        userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        asserter.execute(() -> {
            PanacheMock.verify(UserInfo.class, Mockito.atLeastOnce()).persistOrUpdate(Mockito.<UserInfo> any(), any());
        });
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
        userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        asserter.execute(() -> {
            PanacheMock.verify(UserInfo.class, Mockito.atLeastOnce()).persistOrUpdate(Mockito.<UserInfo> any(), any());
        });
    }

    @Test
    @RunOnVertxContext
    void initOrderStreamWithFoundedUserIdAndInstitutionEmpty(UniAsserter asserter){
        UserInstitution userInstitution = constructUserInstitution();
        userInstitution.setInstitutionId("institutionId2");
        PanacheMock.mock(UserInfo.class);
        UserInfo userInfo = new UserInfo();
        userInfo.setInstitutions(List.of());
        when(UserInfo.findByIdOptional(any()))
                .thenReturn(Uni.createFrom().item(Optional.of(retrieveUserInfo())));
        mockPersistUserInfo(asserter);
        userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        asserter.execute(() -> {
            PanacheMock.verify(UserInfo.class, Mockito.atLeastOnce()).persistOrUpdate(Mockito.<UserInfo> any(), any());
        });

    }

    @Test
    @RunOnVertxContext
    void initOrderStreamWithNotFoundUserId(UniAsserter asserter){
        UserInstitution userInstitution = constructUserInstitution();
        PanacheMock.mock(UserInfo.class);
        when(UserInfo.findByIdOptional(anyString()))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        asserter.execute(() -> {
            PanacheMock.verify(UserInfo.class, Mockito.atLeastOnce()).persistOrUpdate(Mockito.<UserInfo> any(), any());
        });
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

        userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        asserter.execute(() -> {
            PanacheMock.verify(UserInfo.class, Mockito.atLeastOnce()).deleteById(userInfo.getUserId());
        });
    }

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
        userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        asserter.execute(() -> {
            PanacheMock.verify(UserInfo.class, Mockito.times(1)).findByIdOptional(userInstitution.getUserId());
            PanacheMock.verifyNoMoreInteractions(UserInfo.class);
        });
    }


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

        userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        asserter.execute(() -> {
            PanacheMock.verify(UserInfo.class, Mockito.times(1)).findByIdOptional(userInstitution.getUserId());
            PanacheMock.verifyNoMoreInteractions(UserInfo.class);
        });
    }


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

        userInstitutionRepository.updateUser(userInstitution)
                .subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted();

        asserter.execute(() -> {
            PanacheMock.verify(UserInfo.class, Mockito.times(1)).findByIdOptional(userInstitution.getUserId());
            PanacheMock.verify(UserInfo.class, Mockito.atLeastOnce()).persistOrUpdate(Mockito.<UserInfo> any(), any());
            PanacheMock.verifyNoMoreInteractions(UserInfo.class);
        });
    }

    private UserInfo retrieveUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("userId");
        UserInstitutionRole role = new UserInstitutionRole();
        role.setInstitutionId(INSTITUTION_ID);
        role.setInstitutionName("institutionName");
        role.setRole(PartyRole.DELEGATE);
        UserInstitutionRole role1 = new UserInstitutionRole();
        role1.setInstitutionId("institutionId3");
        role1.setInstitutionName("institutionName");
        role1.setRole(PartyRole.DELEGATE);

        List<UserInstitutionRole> userInstitutionRoles = new ArrayList<>();
        userInstitutionRoles.add(role);
        userInstitutionRoles.add(role1);
        userInfo.setInstitutions(userInstitutionRoles);
        return userInfo;
    }

    private UserInstitution constructUserInstitution() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId(INSTITUTION_ID);
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setProductId("productId");
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
        userInstitution.setProducts(List.of(onboardedProduct));
        return userInstitution;
    }



}
