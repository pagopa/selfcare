package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** The scenarios authenticate with really signed tokens: the signature must be what the application verifies. */
class ParityJwtTest {

  private static RSAPublicKey publishedKey() throws Exception {
    String pem =
        ParityJwt.publicKeyPem()
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
  }

  private static DecodedJWT verified(String token) throws Exception {
    return JWT.require(Algorithm.RSA256(publishedKey(), null)).build().verify(token);
  }

  @Test
  void validTokenIsSignedWithTheKeyThatIsPublished() throws Exception {
    DecodedJWT jwt = verified(ParityJwt.token(ParityJwt.User.ADMIN));

    assertEquals("SPID", jwt.getIssuer());
    assertEquals(ParityJwt.User.ADMIN.uid, jwt.getClaim("uid").asString());
    assertEquals("AAAAAA00A00A000A", jwt.getClaim("fiscal_number").asString());
    assertTrue(jwt.getClaim("tenant_id").isMissing());
    assertTrue(jwt.getExpiresAt().toInstant().isAfter(Instant.now()));
    assertFalse(jwt.getIssuedAt() == null);
  }

  @Test
  void tenantClaimIsOnlyPresentWhenRequested() throws Exception {
    assertEquals("AR", verified(ParityJwt.token(ParityJwt.User.ADMIN, "AR")).getClaim("tenant_id").asString());
  }

  @Test
  void usersAreDistinct() {
    assertNotEquals(ParityJwt.User.ADMIN.uid, ParityJwt.User.REQUESTER.uid);
    assertNotEquals(ParityJwt.User.REQUESTER.uid, ParityJwt.User.NO_PERMISSION.uid);
  }

  @Test
  void expiredTokenHasAValidSignatureButIsExpired() throws Exception {
    String token = ParityJwt.expired(ParityJwt.User.ADMIN);

    assertThrows(JWTVerificationException.class, () -> verified(token));
    assertTrue(JWT.decode(token).getExpiresAt().toInstant().isBefore(Instant.now()));
  }

  @Test
  void forgedTokenIsRejectedBySignature() {
    assertThrows(JWTVerificationException.class, () -> verified(ParityJwt.forged(ParityJwt.User.ADMIN)));
  }

  @Test
  void customVariantsChangeExactlyWhatTheyDeclare() throws Exception {
    DecodedJWT noExp = verified(ParityJwt.custom(ParityJwt.User.ADMIN, ParityJwt.Spec::noExpiration));
    assertTrue(noExp.getExpiresAt() == null);
    assertEquals(ParityJwt.User.ADMIN.uid, noExp.getClaim("uid").asString());

    DecodedJWT noIat = verified(ParityJwt.custom(ParityJwt.User.ADMIN, ParityJwt.Spec::noIssuedAt));
    assertTrue(noIat.getIssuedAt() == null);

    DecodedJWT noUid = verified(ParityJwt.custom(ParityJwt.User.ADMIN, ParityJwt.Spec::noUid));
    assertTrue(noUid.getClaim("uid").isMissing());

    DecodedJWT emptyUid = verified(ParityJwt.custom(ParityJwt.User.ADMIN, spec -> spec.uid("")));
    assertEquals("", emptyUid.getClaim("uid").asString());

    DecodedJWT noFiscalNumber = verified(ParityJwt.custom(ParityJwt.User.ADMIN, ParityJwt.Spec::noFiscalNumber));
    assertTrue(noFiscalNumber.getClaim("fiscal_number").isMissing());

    assertEquals("PAGOPA", verified(ParityJwt.withIssuer(ParityJwt.User.ADMIN, "PAGOPA")).getIssuer());
    assertTrue(JWT.decode(ParityJwt.custom(ParityJwt.User.ADMIN, spec -> spec.issuer(null))).getIssuer() == null);
  }

  @Test
  void notBeforeInTheFutureIsHonouredByTheVerifier() {
    String token = ParityJwt.custom(ParityJwt.User.ADMIN, spec -> spec.notBefore(Instant.now().plusSeconds(600)));

    assertThrows(JWTVerificationException.class, () -> verified(token));
  }

  @Test
  void unsignedTokenHasNoSignatureAndAlgNone() {
    String token = ParityJwt.custom(ParityJwt.User.ADMIN, ParityJwt.Spec::unsigned);

    assertEquals("none", JWT.decode(token).getAlgorithm());
    assertTrue(token.endsWith("."), "an unsigned token must end with an empty signature");
    assertThrows(JWTVerificationException.class, () -> verified(token));
  }
}
