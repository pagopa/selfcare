package it.pagopa.selfcare.onboarding.util;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.security.Principal;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;

public final class SecurityIdentityUtils {

    public static final String CLAIM_UID = "uid";

    private SecurityIdentityUtils() {
    }

    /**
     * User id of the caller: the {@code uid} claim of the verified JWT, as the Spring BFF does
     * for {@code SelfCareUser.getId()}. The name of the JWT principal cannot be used because
     * selfcare tokens carry no {@code sub} claim.
     */
    public static String getUid(SecurityIdentity securityIdentity) {
        if (securityIdentity == null) {
            return null;
        }
        Principal principal = securityIdentity.getPrincipal();
        if (principal instanceof JsonWebToken jwt) {
            return claimAsString(jwt, CLAIM_UID);
        }
        return attributeAsString(securityIdentity, CLAIM_UID);
    }

    public static String getFiscalCode(SecurityIdentity securityIdentity) {
        Principal principal = securityIdentity.getPrincipal();
        if (principal instanceof JsonWebToken jwt) {
            return firstNonBlank(
                    claimAsString(jwt, "fiscal_number"),
                    claimAsString(jwt, "fiscalCode"),
                    claimAsString(jwt, "fiscal_code"));
        }
        return firstNonBlank(
                attributeAsString(securityIdentity, "fiscal_number"),
                attributeAsString(securityIdentity, "fiscalCode"),
                attributeAsString(securityIdentity, "fiscal_code"));
    }

    /** String value of a claim; {@code null} when absent, JSON null or not a string. */
    public static String claimAsString(JsonWebToken jwt, String name) {
        Object value = rawClaim(jwt, name);
        return value instanceof String string ? string : null;
    }

    /** Value of a claim, with {@code null} for absent claims and JSON null; JSON strings are unwrapped. */
    public static Object rawClaim(JsonWebToken jwt, String name) {
        Object value = jwt.getClaim(name);
        if (value == null || JsonValue.NULL.equals(value)) {
            return null;
        }
        return value instanceof JsonString jsonString ? jsonString.getString() : value;
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
