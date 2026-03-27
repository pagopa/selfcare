package it.pagopa.selfcare.onboarding.core.utils;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;

import java.util.List;
import java.util.Optional;

public class Utils {
    private Utils() {
    }

    public static boolean isUserAdmin(String userTaxCode, List<User> users) {
        return users.stream()
                .anyMatch(user -> user.getTaxCode().equals(userTaxCode) &&
                        (user.getRole() == PartyRole.MANAGER
                        || user.getRole() == PartyRole.DELEGATE
                        || user.getRole() == PartyRole.SUB_DELEGATE)
                );
    }

    public static Optional<User> getManager(List<User> users) {
        return users.stream()
                .filter(user -> user.getRole() == PartyRole.MANAGER)
                .findFirst();
    }
}
