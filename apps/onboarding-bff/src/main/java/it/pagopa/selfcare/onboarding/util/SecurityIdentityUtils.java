package it.pagopa.selfcare.onboarding.util;

import io.quarkus.security.identity.SecurityIdentity;
import java.security.Principal;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;

public final class SecurityIdentityUtils {

    private SecurityIdentityUtils() {
    }

    public static String getUid(SecurityIdentity securityIdentity) {
        Principal principal = securityIdentity.getPrincipal();
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        Object uid = securityIdentity.getAttribute("uid");
        return uid != null ? uid.toString() : null;
    }

    public static String getFiscalCode(SecurityIdentity securityIdentity) {
        Principal principal = securityIdentity.getPrincipal();
        if (principal instanceof JsonWebToken jwt) {
            return firstNonBlank(
                    jwt.getClaim("fiscal_number"),
                    jwt.getClaim("fiscalCode"),
                    jwt.getClaim("fiscal_code"));
        }
        return firstNonBlank(
                attributeAsString(securityIdentity, "fiscal_number"),
                attributeAsString(securityIdentity, "fiscalCode"),
                attributeAsString(securityIdentity, "fiscal_code"));
    }

    private static String attributeAsString(SecurityIdentity securityIdentity, String key) {
        Object value = securityIdentity.getAttribute(key);
        return value == null ? null : value.toString();
    }

    private static String firstNonBlank(String... values) {
        return Optional.ofNullable(values)
                .stream()
                .flatMap(java.util.Arrays::stream)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }
}
