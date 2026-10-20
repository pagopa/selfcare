package it.pagopa.selfcare.onboarding.client.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Instant;
import java.util.Locale;

/** Performs fail-fast consistency checks on the configured machine token. */
final class TechnicalJwtValidator {

    private static final String EXPECTED_ALGORITHM = "RS256";
    private static final String TENANT_CLAIM = "tenant_id";
    private static final String UID_CLAIM = "uid";

    private TechnicalJwtValidator() {}

    /**
     * Checks claims that the caller can validate without the public key. Signature verification remains
     * authoritative in onboarding-functions.
     */
    static void validate(String token, String expectedTenant) {
        if (token == null || token.isBlank()) {
            throw invalid("token is missing");
        }
        try {
            DecodedJWT jwt = JWT.decode(token);
            if (!EXPECTED_ALGORITHM.equals(jwt.getAlgorithm())) {
                throw invalid("algorithm must be RS256");
            }
            if (jwt.getExpiresAtAsInstant() == null
                    || !jwt.getExpiresAtAsInstant().isAfter(Instant.now())) {
                throw invalid("token is expired or has no exp claim");
            }
            if (isBlank(jwt.getIssuer())) {
                throw invalid("iss claim is missing");
            }
            if (isBlank(jwt.getClaim(UID_CLAIM).asString())) {
                throw invalid("uid claim is missing");
            }
            String tokenTenant = normalize(jwt.getClaim(TENANT_CLAIM).asString());
            if (tokenTenant == null || !tokenTenant.equals(normalize(expectedTenant))) {
                throw invalid("tenant_id does not match X-Tenant-Id");
            }
        } catch (JWTDecodeException exception) {
            throw invalid("token is malformed", exception);
        }
    }

    private static String normalize(String value) {
        return isBlank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("Invalid technical JWT for onboarding-functions: " + reason);
    }

    private static IllegalStateException invalid(String reason, RuntimeException cause) {
        return new IllegalStateException(
                "Invalid technical JWT for onboarding-functions: " + reason, cause);
    }
}
