package it.pagopa.selfcare.cucumber.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.pagopa.selfcare.cucumber.utils.model.JwtData;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@ApplicationScoped
public class TestJwtGenerator {

    private final RSAPrivateKey privateKey;

    public TestJwtGenerator() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        this.privateKey = readPrivateKey();
    }

    public String generateToken(JwtData jwtData) {
        return generateToken(jwtData, Instant.now().plusSeconds(3600));
    }

    public String generateToken(JwtData jwtData, Instant expiresAt) {
        return Optional.ofNullable(jwtData).map(jd -> {
                    final JWTCreator.Builder builder = JWT.create()
                            .withHeader(jwtData.getJwtHeader())
                            .withPayload(jwtData.getJwtPayload())
                            .withIssuedAt(Instant.now());
                    Optional.ofNullable(expiresAt).ifPresent(builder::withExpiresAt);
                    return builder.sign(Algorithm.RSA256(privateKey));
                }
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

    public static void main(String[] args) {
        try {
            final TestJwtGenerator jwtGenerator = new TestJwtGenerator();
            final JwtData jwtData = new JwtData();
            jwtData.setJwtPayload(new HashMap<>());
            jwtData.setJwtHeader(new HashMap<>());

            final Scanner scanner = new Scanner(System.in);
            // FISCAL_NUMBER
            System.out.print("fiscal_number: ");
            final String fiscalNumber = scanner.nextLine();
            jwtData.getJwtPayload().put("fiscal_number", fiscalNumber);
            // NAME
            System.out.print("name: ");
            final String name = scanner.nextLine();
            jwtData.getJwtPayload().put("name", name);
            // FAMILY_NAME
            System.out.print("family_name: ");
            final String familyName = scanner.nextLine();
            jwtData.getJwtPayload().put("family_name", familyName);
            // UID
            System.out.print("uid (Leave empty to autogenerate one): ");
            final String uid = scanner.nextLine();
            jwtData.getJwtPayload().put("uid", uid.isBlank() ? UUID.randomUUID().toString() : uid);
            // EMAIL
            System.out.print("email: ");
            final String email = scanner.nextLine();
            jwtData.getJwtPayload().put("email", email);
            // ORGANIZATION
            System.out.print("organization (json file path): ");
            final String organizationFilePath = scanner.nextLine();
            final ObjectReader organizationReader = new ObjectMapper().readerFor(Map.class);
            final Map<String, String> organization = organizationReader.readValue(new File(organizationFilePath));
            jwtData.getJwtPayload().put("organization", organization);
            // AUD
            System.out.print("aud: ");
            final String aud = scanner.nextLine();
            jwtData.getJwtPayload().put("aud", aud);
            // KID
            System.out.print("kid: ");
            final String kid = scanner.nextLine();
            jwtData.getJwtHeader().put("kid", kid);
            scanner.close();

            // SPID_LEVEL
            jwtData.getJwtPayload().put("spid_level", "https://www.spid.gov.it/SpidL2");
            // ISS
            jwtData.getJwtPayload().put("iss", "https://selfcare.pagopa.it");
            // JTI
            jwtData.getJwtPayload().put("jti", UUID.randomUUID().toString());

            // OUTPUT
            System.out.println(jwtGenerator.generateToken(jwtData, null));
        } catch (Exception ex) {
            System.err.printf("Error generating token: %s%n", ex);
        }
    }

}
