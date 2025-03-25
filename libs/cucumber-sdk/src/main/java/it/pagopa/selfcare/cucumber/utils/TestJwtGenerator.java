package it.pagopa.selfcare.cucumber.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import it.pagopa.selfcare.cucumber.utils.model.JwtData;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Component
@ApplicationScoped
public class TestJwtGenerator {

    private final RSAPrivateKey privateKey;

    public TestJwtGenerator() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        this.privateKey = readPrivateKey();
    }

    public String generateToken(JwtData jwtData) {
        return Optional.ofNullable(jwtData).map(jd ->
                JWT.create()
                        .withHeader(jwtData.getJwtHeader())
                        .withPayload(jwtData.getJwtPayload())
                        .withIssuedAt(Instant.now())
                        .withExpiresAt(Instant.now().plusSeconds(3600))
                        .sign(Algorithm.RSA256(privateKey))
        ).orElse(null);
    }

    private RSAPrivateKey readPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        log.info("Reading private key");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("key/private-key.pem")) {
            if (inputStream == null) {
                throw new IOException("File not found in classpath");
            }
            String privateKeyString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            privateKeyString = privateKeyString.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s+", "");
            byte[] pKeyEncoded = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pKeyEncoded);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        }
    }
}
