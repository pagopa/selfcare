package it.pagopa.selfcare.mscore.web.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

final class JwtTestSupport {

    private static final byte[] TEST_ONLY_SECRET =
            "test-only-secret-at-least-256-bits-long".getBytes(StandardCharsets.UTF_8);

    private JwtTestSupport() {
    }

    static Stream<String> arAudiences() {
        return Stream.of(
                "api.dev.selfcare.pagopa.it",
                "api.uat.selfcare.pagopa.it",
                "api.selfcare.pagopa.it");
    }

    /**
     * PNPG audiences recognised by the current rule. The UAT session audience
     * {@code imprese.uat.notifichedigitali.it} is deliberately missing: see
     * {@code EncryptedTaxCodeParamResolverTest#uatPnpgSessionAudienceIsNotRecognised}.
     */
    static Stream<String> pnpgAudiences() {
        return Stream.of(
                "pnpg.dev.selfcare.pagopa.it",
                "api-pnpg.dev.selfcare.pagopa.it",
                "api-pnpg.uat.selfcare.pagopa.it",
                "api-pnpg.selfcare.pagopa.it",
                "imprese.notifichedigitali.it",
                "IMPRESE.NOTIFICHEDIGITALI.IT");
    }

    static void authenticateWithAudience(String... audience) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().subject("test-user");
        if (audience.length > 0) {
            claims.audience(List.of(audience));
        }
        authenticateWithToken(sign(claims.build()));
    }

    static void authenticateWithToken(String token) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-user", token));
    }

    private static String sign(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(TEST_ONLY_SECRET));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
