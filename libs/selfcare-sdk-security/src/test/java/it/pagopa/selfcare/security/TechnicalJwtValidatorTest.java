package it.pagopa.selfcare.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TechnicalJwtValidatorTest {

    @Test
    void acceptsTenantBoundUnexpiredRs256Token() {
        // given
        String token = token("RS256", Instant.now().plusSeconds(300), "PAGOPA", "fake-machine", "AR");

        // when
        var validation = (org.junit.jupiter.api.function.Executable)
                () -> TechnicalJwtValidator.validate(token, "AR");

        // then
        assertDoesNotThrow(validation);
    }

    @Test
    void rejectsExpiredTechnicalToken() {
        // given
        String token = token("RS256", Instant.now().minusSeconds(1), "PAGOPA", "fake-machine", "AR");

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> TechnicalJwtValidator.validate(token, "AR"));

        // then
        assertEquals(
                "Invalid technical JWT for onboarding-functions: token is expired or has no exp claim",
                exception.getMessage());
    }

    @Test
    void rejectsTechnicalTokenForAnotherTenant() {
        // given
        String token = token("RS256", Instant.now().plusSeconds(300), "PAGOPA", "fake-machine", "PNPG");

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> TechnicalJwtValidator.validate(token, "AR"));

        // then
        assertEquals(
                "Invalid technical JWT for onboarding-functions: tenant_id does not match X-Tenant-Id",
                exception.getMessage());
    }

    @Test
    void rejectsTechnicalTokenWithoutIssuerOrUid() {
        // given
        Instant expiration = Instant.now().plusSeconds(300);
        String missingIssuer = token("RS256", expiration, "", "fake-machine", "AR");
        String missingUid = token("RS256", expiration, "PAGOPA", "", "AR");

        // when
        IllegalStateException issuerException = assertThrows(
                IllegalStateException.class,
                () -> TechnicalJwtValidator.validate(missingIssuer, "AR"));
        IllegalStateException uidException = assertThrows(
                IllegalStateException.class,
                () -> TechnicalJwtValidator.validate(missingUid, "AR"));

        // then
        assertEquals(
                "Invalid technical JWT for onboarding-functions: iss claim is missing",
                issuerException.getMessage());
        assertEquals(
                "Invalid technical JWT for onboarding-functions: uid claim is missing",
                uidException.getMessage());
    }

    @Test
    void rejectsMalformedOrNonRs256TechnicalToken() {
        // given
        String wrongAlgorithm =
                token("HS256", Instant.now().plusSeconds(300), "PAGOPA", "fake-machine", "AR");

        // when
        IllegalStateException malformedException = assertThrows(
                IllegalStateException.class,
                () -> TechnicalJwtValidator.validate("not-a-jwt", "AR"));
        IllegalStateException algorithmException = assertThrows(
                IllegalStateException.class,
                () -> TechnicalJwtValidator.validate(wrongAlgorithm, "AR"));

        // then
        assertEquals(
                "Invalid technical JWT for onboarding-functions: token is malformed",
                malformedException.getMessage());
        assertEquals(
                "Invalid technical JWT for onboarding-functions: algorithm must be RS256",
                algorithmException.getMessage());
    }

    private static String token(
            String algorithm, Instant expiration, String issuer, String uid, String tenant) {
        String header = "{\"alg\":\"" + algorithm + "\",\"typ\":\"JWT\"}";
        String payload = String.format(
                "{\"exp\":%d,\"iss\":\"%s\",\"uid\":\"%s\",\"tenant_id\":\"%s\"}",
                expiration.getEpochSecond(), issuer, uid, tenant);
        return encode(header) + "." + encode(payload) + "." + encode("fake-signature");
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
