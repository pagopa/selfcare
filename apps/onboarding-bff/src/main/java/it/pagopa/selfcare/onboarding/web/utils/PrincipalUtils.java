package it.pagopa.selfcare.onboarding.web.utils;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import java.lang.reflect.Method;
import java.security.Principal;

public final class PrincipalUtils {

    private PrincipalUtils() {
    }

    public static SelfCareUser getSelfCareUser(Principal principal) {
        if (principal instanceof SelfCareUser selfCareUser) {
            return selfCareUser;
        }

        Object innerPrincipal = extractInnerPrincipal(principal);
        if (innerPrincipal instanceof SelfCareUser selfCareUser) {
            return selfCareUser;
        }

        throw new IllegalStateException("Unsupported principal type: " + (principal == null ? "null" : principal.getClass().getName()));
    }

    private static Object extractInnerPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }
        try {
            Method method = principal.getClass().getMethod("getPrincipal");
            return method.invoke(principal);
        } catch (Exception ignored) {
            return null;
        }
    }
}
