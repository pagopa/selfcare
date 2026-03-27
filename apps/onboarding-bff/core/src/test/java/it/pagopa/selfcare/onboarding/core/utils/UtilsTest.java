package it.pagopa.selfcare.onboarding.core.utils;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
    @ParameterizedTest
    @CsvSource({
            "MANAGER",
            "DELEGATE",
            "SUB_DELEGATE"
    })
    void isUserAdmin_shouldReturnTrueForAdminRoles(PartyRole role) {
        User user = new User();
        user.setTaxCode("userTaxCode");
        user.setRole(role);
        List<User> users = List.of(user);

        boolean result = Utils.isUserAdmin("userTaxCode", users);

        assertTrue(result);
    }

    @Test
    void isUserAdmin_shouldReturnFalseWhenUserIsNotAdmin() {
        User user = new User();
        user.setTaxCode("userTaxCode");
        user.setRole(PartyRole.OPERATOR);
        List<User> users = List.of(user);

        boolean result = Utils.isUserAdmin("userTaxCode", users);

        assertFalse(result);
    }

    @Test
    void isUserAdmin_shouldReturnFalseWhenUserNotFound() {
        User user = new User();
        user.setTaxCode("anotherTaxCode");
        user.setRole(PartyRole.MANAGER);
        List<User> users = List.of(user);

        boolean result = Utils.isUserAdmin("userTaxCode", users);

        assertFalse(result);
    }

    @Test
    void getManager_shouldReturnManagerWhenPresent() {
        User user = new User();
        user.setRole(PartyRole.MANAGER);
        List<User> users = List.of(user);

        Optional<User> result = Utils.getManager(users);

        assertTrue(result.isPresent());
        assertEquals(PartyRole.MANAGER, result.get().getRole());
    }

    @Test
    void getManager_shouldReturnEmptyWhenManagerNotPresent() {
        User user = new User();
        user.setRole(PartyRole.OPERATOR);
        List<User> users = List.of(user);

        Optional<User> result = Utils.getManager(users);

        assertFalse(result.isPresent());
    }

    @Test
    void getManager_shouldReturnEmptyWhenUsersListIsEmpty() {
        List<User> users = List.of();

        Optional<User> result = Utils.getManager(users);

        assertFalse(result.isPresent());
    }
}