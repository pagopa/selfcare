package it.pagopa.selfcare.onboarding.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;

@Slf4j
public class JwtUtils {

  public static String generateToken(JwtData jwtData) {
    if (Objects.nonNull(jwtData)) {
      try {
        File file = new File("src/test/resources/certs/sk-key.pem");
        Algorithm alg =
            Algorithm.RSA256(getPrivateKey(new String(Files.readAllBytes(file.toPath()))));
        String jwt =
            JWT.create()
                .withHeader(jwtData.getJwtHeader())
                .withPayload(jwtData.getJwtPayload())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(alg);
        log.debug("Generated custom jwt token");
        return jwt;

      } catch (Exception e) {
        log.error("Error into generateToken: ", e);
      }
    }
    return null;
  }

  public static RSAPrivateKey getPrivateKey(String privateKey) {
    privateKey =
        privateKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
    byte[] pKeyEncoded = Base64.decode(privateKey);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pKeyEncoded);
    KeyFactory kf;
    try {
      kf = KeyFactory.getInstance("RSA");
      return (RSAPrivateKey) kf.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }
}
