package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import static org.junit.jupiter.api.Assertions.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.InternalException;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.JwtConfig;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

class AssertionGeneratorVisuraTest {
  private final AssertionGeneratorVisura assertionGenerator = new AssertionGeneratorVisura();
    private String privateKey;
    private final JwtConfig jwtConfig = JwtConfig.builder()
            .issuer("Issuer")
            .subject("Subject")
            .audience("Audience")
            .kid("Kid")
            .purposeId("PurposeId")
            .build();

    @Test
    void generateClientAssertionSuccess() throws Exception {
        privateKey = new String(Files.readAllBytes(Paths.get("src/test/resources/private_key.txt")));
        String publicKey = new String(Files.readAllBytes(Paths.get("src/test/resources/public_key.txt")));

        String jwt = assertionGenerator.generateClientAssertion(jwtConfig, privateKey);

        DecodedJWT decodecJwt = JWT.decode(jwt);
        assertEquals("RS256", decodecJwt.getAlgorithm());
        assertEquals("Issuer", decodecJwt.getIssuer());
        assertEquals("Subject", decodecJwt.getSubject());
        assertEquals("Audience", decodecJwt.getAudience().get(0));
        assertEquals("Kid", decodecJwt.getKeyId());
        assertEquals("PurposeId", decodecJwt.getClaim("purposeId").asString());
        assertTrue(decodecJwt.getExpiresAt().getTime() > System.currentTimeMillis());
        assertTrue(decodecJwt.getIssuedAt().getTime() < System.currentTimeMillis());
        RSAPublicKey pubKey = KeyGenerator.getPublicKey(publicKey);
        final Algorithm algorithm = Algorithm.RSA256(pubKey, null);
        JWTVerifier verifier = JWT.require(algorithm)
                .build();
        assertDoesNotThrow(() -> verifier.verify(jwt));
    }

    @Test
    void generateClientAssertionError() {
        privateKey = "testtesttesttesttesttesttesttesttest";
        assertThrows(InternalException.class, () -> assertionGenerator.generateClientAssertion(jwtConfig, privateKey));
    }
}