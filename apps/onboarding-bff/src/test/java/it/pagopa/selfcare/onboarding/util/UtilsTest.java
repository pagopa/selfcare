package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UtilsTest {
    @ParameterizedTest
    @CsvSource({"MANAGER", "DELEGATE", "SUB_DELEGATE"})
    void isUserAdmin_shouldReturnTrueForAdminRoles(PartyRole role) {
        User user = new User();
        user.setTaxCode("userTaxCode");
        user.setRole(role);

        boolean result = Utils.isUserAdmin("userTaxCode", List.of(user));

        assertTrue(result);
    }

    @Test
    void isUserAdmin_shouldReturnFalseWhenUserIsNotAdmin() {
        User user = new User();
        user.setTaxCode("userTaxCode");
        user.setRole(PartyRole.OPERATOR);

        assertFalse(Utils.isUserAdmin("userTaxCode", List.of(user)));
    }

    @Test
    void getManager_shouldReturnManagerWhenPresent() {
        User user = new User();
        user.setRole(PartyRole.MANAGER);

        Optional<User> result = Utils.getManager(List.of(user));

        assertTrue(result.isPresent());
        assertEquals(PartyRole.MANAGER, result.get().getRole());
    }

    @Test
    void getManager_shouldReturnEmptyWhenManagerNotPresent() {
        User user = new User();
        user.setRole(PartyRole.OPERATOR);

        assertFalse(Utils.getManager(List.of(user)).isPresent());
    }
}
