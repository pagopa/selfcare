package it.pagopa.selfcare.onboarding;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.AggregateInstitution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapperImpl;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openapi.quarkus.core_json.model.DelegationResponse;

import java.util.Collections;
import java.util.List;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PN;

@QuarkusTest
class OnboardingMapperTest {

    @Inject
    private OnboardingMapperImpl onboardingMapper;

    @Test
    void mapToOnboardingAggregateOrchestratorInputTestWithAggregateUsers(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId("productId");
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        User user2 = new User();
        user2.setId("aggregateUser");
        user2.setRole(PartyRole.MANAGER);
        user2.setProductRole("aggregateProductRole");
        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(List.of(user2));
        onboarding.setAggregates(List.of(aggregateInstitution));

        OnboardingAggregateOrchestratorInput response = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregateInstitution);
        Assertions.assertEquals(1, response.getUsers().size());
        Assertions.assertEquals("aggregateUser", response.getUsers().get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, response.getUsers().get(0).getRole());
        Assertions.assertEquals("aggregateProductRole", response.getUsers().get(0).getProductRole());

    }

    @Test
    void mapToOnboardingAggregateOrchestratorInputTestWithoutAggregatesUser(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId("productId");
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);
        onboarding.setAggregates(List.of(aggregateInstitution));

        OnboardingAggregateOrchestratorInput response = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregateInstitution);
        Assertions.assertEquals(1, response.getUsers().size());
        Assertions.assertEquals("aggregatorUser", response.getUsers().get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, response.getUsers().get(0).getRole());
        Assertions.assertEquals("aggregatorProductRole", response.getUsers().get(0).getProductRole());
    }

    @ParameterizedTest
    @EnumSource(value = ProductId.class, names = {"PROD_PAGOPA", "PROD_IO"})
    void mapToOnboardingAggregateOrchestratorInputTestWithoutAggregatesUserProdPagoPA(ProductId productId){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId(productId.getValue());
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);
        onboarding.setAggregates(List.of(aggregateInstitution));

        OnboardingAggregateOrchestratorInput response = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregateInstitution);
        Assertions.assertEquals(0, response.getUsers().size());
    }

    @ParameterizedTest
    @EnumSource(value = ProductId.class, names = {"PROD_PAGOPA", "PROD_IO"})
    void mapToOnboardingFromDelegationTest_ADMIN_EA(ProductId productId){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId(productId.getValue());
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        DelegationResponse delegationResponse = new DelegationResponse();
        delegationResponse.setId("delegationId");

        Onboarding response = onboardingMapper.mapToOnboardingFromDelegation(onboarding, delegationResponse);
        Assertions.assertEquals(1, response.getUsers().size());
        Assertions.assertEquals("aggregatorUser", response.getUsers().get(0).getId());
        Assertions.assertEquals(PartyRole.ADMIN_EA, response.getUsers().get(0).getRole());
        Assertions.assertEquals("aggregatorProductRole", response.getUsers().get(0).getProductRole());
        Assertions.assertEquals(delegationResponse.getId(), response.getDelegationId());
    }

    @Test
    void mapToOnboardingFromDelegationTest(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId(PROD_PN.getValue());
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        DelegationResponse delegationResponse = new DelegationResponse();
        delegationResponse.setId("delegationId");

        Onboarding response = onboardingMapper.mapToOnboardingFromDelegation(onboarding, delegationResponse);
        Assertions.assertEquals(1, response.getUsers().size());
        Assertions.assertEquals("aggregatorUser", response.getUsers().get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, response.getUsers().get(0).getRole());
        Assertions.assertEquals("aggregatorProductRole", response.getUsers().get(0).getProductRole());
        Assertions.assertEquals(delegationResponse.getId(), response.getDelegationId());
    }


    @Test
    void mapToOnboardingAggregateOrchestratorInputTestWithAggregatesUserEmptyList(){
        Onboarding onboarding = new Onboarding();
        onboarding.setId("example");
        onboarding.setProductId("productId");
        User user = new User();
        user.setId("aggregatorUser");
        user.setRole(PartyRole.MANAGER);
        user.setProductRole("aggregatorProductRole");
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(Collections.emptyList());
        onboarding.setAggregates(List.of(aggregateInstitution));

        OnboardingAggregateOrchestratorInput response = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregateInstitution);
        Assertions.assertEquals(1, response.getUsers().size());
        Assertions.assertEquals("aggregatorUser", response.getUsers().get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, response.getUsers().get(0).getRole());
        Assertions.assertEquals("aggregatorProductRole", response.getUsers().get(0).getProductRole());
    }

    // Direct tests for mapAggregateUsers method
    @Test
    void mapAggregateUsers_shouldReturnAggregateUsersWhenAvailable() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-test");
        User onboardingUser = new User();
        onboardingUser.setId("onboardingUserId");
        onboardingUser.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(onboardingUser));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        User aggregateUser = new User();
        aggregateUser.setId("aggregateUserId");
        aggregateUser.setRole(PartyRole.DELEGATE);
        aggregateInstitution.setUsers(List.of(aggregateUser));

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("aggregateUserId", result.get(0).getId());
        Assertions.assertEquals(PartyRole.DELEGATE, result.get(0).getRole());
    }

    @Test
    void mapAggregateUsers_shouldReturnEmptyListWhenProdPagopaAndNoAggregateUsers() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        User user = new User();
        user.setId("userId");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void mapAggregateUsers_shouldReturnEmptyListWhenProdIoAndNoAggregateUsers() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(ProductId.PROD_IO.getValue());
        User user = new User();
        user.setId("userId");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(Collections.emptyList());

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void mapAggregateUsers_shouldReturnOnboardingUsersWhenOtherProductAndNoAggregateUsers() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(ProductId.PROD_PN.getValue());
        User user = new User();
        user.setId("onboardingUserId");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("onboardingUserId", result.get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, result.get(0).getRole());
    }

    @Test
    void mapAggregateUsers_shouldReturnOnboardingUsersWhenAggregateInstitutionIsNull() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-test");
        User user = new User();
        user.setId("onboardingUserId");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, null);

        // Then
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("onboardingUserId", result.get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, result.get(0).getRole());
    }

    @Test
    void mapAggregateUsers_shouldReturnEmptyListWhenProdPagopaAndAggregateInstitutionIsNull() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(ProductId.PROD_PAGOPA.getValue());
        User user = new User();
        user.setId("userId");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, null);

        // Then
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void mapAggregateUsers_shouldReturnEmptyListWhenProdIoAndAggregateInstitutionIsNull() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(ProductId.PROD_IO.getValue());
        User user = new User();
        user.setId("userId");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, null);

        // Then
        Assertions.assertEquals(0, result.size());
    }

    @ParameterizedTest
    @EnumSource(value = ProductId.class, names = {"PROD_PAGOPA", "PROD_IO"})
    void mapAggregateUsers_shouldPrioritizeAggregateUsersOverProductCheck(ProductId productId) {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(productId.getValue());
        User onboardingUser = new User();
        onboardingUser.setId("onboardingUserId");
        onboarding.setUsers(List.of(onboardingUser));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        User aggregateUser = new User();
        aggregateUser.setId("aggregateUserId");
        aggregateUser.setRole(PartyRole.DELEGATE);
        aggregateInstitution.setUsers(List.of(aggregateUser));

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then - Should return aggregate users even for PROD_PAGOPA/PROD_IO when aggregate users exist
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("aggregateUserId", result.get(0).getId());
        Assertions.assertEquals(PartyRole.DELEGATE, result.get(0).getRole());
    }

    @Test
    void mapAggregateUsers_shouldReturnMultipleAggregateUsersWhenPresent() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-test");
        User onboardingUser = new User();
        onboardingUser.setId("onboardingUserId");
        onboarding.setUsers(List.of(onboardingUser));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        User aggregateUser1 = new User();
        aggregateUser1.setId("aggregateUserId1");
        aggregateUser1.setRole(PartyRole.DELEGATE);
        User aggregateUser2 = new User();
        aggregateUser2.setId("aggregateUserId2");
        aggregateUser2.setRole(PartyRole.MANAGER);
        aggregateInstitution.setUsers(List.of(aggregateUser1, aggregateUser2));

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("aggregateUserId1", result.get(0).getId());
        Assertions.assertEquals(PartyRole.DELEGATE, result.get(0).getRole());
        Assertions.assertEquals("aggregateUserId2", result.get(1).getId());
        Assertions.assertEquals(PartyRole.MANAGER, result.get(1).getRole());
    }

    @Test
    void mapAggregateUsers_shouldReturnNullWhenOnboardingUsersIsNull() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-test");
        onboarding.setUsers(null); // Null users

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void mapAggregateUsers_shouldReturnEmptyListWhenOnboardingUsersIsEmpty() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-test");
        onboarding.setUsers(Collections.emptyList()); // Empty users

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void mapAggregateUsers_shouldReturnOnboardingUsersWhenProductIdIsNull() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(null); // Null product ID
        User user = new User();
        user.setId("onboardingUserId");
        user.setRole(PartyRole.MANAGER);
        onboarding.setUsers(List.of(user));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("onboardingUserId", result.get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, result.get(0).getRole());
    }

    @Test
    void mapAggregateUsers_shouldReturnMultipleOnboardingUsersWhenNoAggregateUsers() {
        // Given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-test");
        User user1 = new User();
        user1.setId("onboardingUserId1");
        user1.setRole(PartyRole.MANAGER);
        User user2 = new User();
        user2.setId("onboardingUserId2");
        user2.setRole(PartyRole.DELEGATE);
        onboarding.setUsers(List.of(user1, user2));

        AggregateInstitution aggregateInstitution = new AggregateInstitution();
        aggregateInstitution.setUsers(null);

        // When
        List<User> result = onboardingMapper.mapAggregateUsers(onboarding, aggregateInstitution);

        // Then
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("onboardingUserId1", result.get(0).getId());
        Assertions.assertEquals(PartyRole.MANAGER, result.get(0).getRole());
        Assertions.assertEquals("onboardingUserId2", result.get(1).getId());
        Assertions.assertEquals(PartyRole.DELEGATE, result.get(1).getRole());
    }
}
