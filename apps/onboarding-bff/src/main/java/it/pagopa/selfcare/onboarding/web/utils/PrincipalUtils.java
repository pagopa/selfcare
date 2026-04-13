package it.pagopa.selfcare.onboarding.web.utils;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.web.security.JwtAuthenticationToken;
import java.security.Principal;

public final class PrincipalUtils {

    private PrincipalUtils() {
    }

    public static SelfCareUser getSelfCareUser(Principal principal) {
        if (principal instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Object innerPrincipal = jwtAuthenticationToken.getPrincipal();
            if (innerPrincipal instanceof SelfCareUser selfCareUser) {
                return selfCareUser;
            }
        }

        if (principal instanceof SelfCareUser selfCareUser) {
            return selfCareUser;
        }

        throw new IllegalStateException("Unsupported principal type: " + (principal == null ? "null" : principal.getClass().getName()));
    }
}
