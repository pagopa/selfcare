package it.pagopa.selfcare.onboarding.security;

import it.pagopa.selfcare.onboarding.util.SecurityIdentityUtils;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * The identity claims the Spring BFF reads as strings to build its {@code SelfCareUser}: a token
 * carrying any of them with another JSON type is not authenticated.
 */
final class IdentityClaims {

    private static final String ISSUER_SPID = "SPID";
    private static final List<String> COMMON = List.of("uid", "email", "name", "family_name");

    private IdentityClaims() {
    }

    static boolean areStrings(JsonWebToken jwt) {
        return COMMON.stream().allMatch(claim -> isStringOrAbsent(jwt, claim))
                && (!ISSUER_SPID.equals(jwt.getIssuer()) || isStringOrAbsent(jwt, "fiscal_number"));
    }

    private static boolean isStringOrAbsent(JsonWebToken jwt, String claim) {
        Object value = SecurityIdentityUtils.rawClaim(jwt, claim);
        return value == null || value instanceof String;
    }
}
