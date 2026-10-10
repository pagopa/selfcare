package it.pagopa.selfcare.onboarding.parity;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.JWTCreator;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/** Real RS256 tokens for the parity harness; the application verifies them with {@link #publicKeyPem()}. */
public final class ParityJwt {

  /** Users the scenarios authenticate as. IAM/onboarding fixtures decide what each one may do. */
  public enum User {
    ADMIN("11111111-1111-4111-8111-111111111111", "Jane", "Admin", "jane.admin@example.test", "AAAAAA00A00A000A"),
    REQUESTER("22222222-2222-4222-8222-222222222222", "Rocky", "Requester", "rocky.requester@example.test", "BBBBBB00B00B000B"),
    NO_PERMISSION("33333333-3333-4333-8333-333333333333", "Nina", "Nobody", "nina.nobody@example.test", "CCCCCC00C00C000C");

    public final String uid;
    final String name;
    final String familyName;
    final String email;
    final String fiscalNumber;

    User(String uid, String name, String familyName, String email, String fiscalNumber) {
      this.uid = uid;
      this.name = name;
      this.familyName = familyName;
      this.email = email;
      this.fiscalNumber = fiscalNumber;
    }
  }

  private static final KeyPair SIGNING_KEY = generate();
  private static final KeyPair FOREIGN_KEY = generate();

  private ParityJwt() {}

  private static KeyPair generate() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("RSA is required for parity test tokens", e);
    }
  }

  public static String publicKeyPem() {
    String body =
        Base64.getMimeEncoder(64, "\n".getBytes())
            .encodeToString(SIGNING_KEY.getPublic().getEncoded());
    return "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----";
  }

  /** Valid SPID token for the given user; {@code tenant} may be null (token without tenant_id claim). */
  public static String token(User user, String tenant) {
    return create(user, "SPID", tenant, Instant.now().plusSeconds(3600), SIGNING_KEY);
  }

  public static String token(User user) {
    return token(user, null);
  }

  public static String expired(User user) {
    return create(user, "SPID", null, Instant.now().minusSeconds(3600), SIGNING_KEY);
  }

  /** Correct shape but signed with a key the application does not trust. */
  public static String forged(User user) {
    return create(user, "SPID", null, Instant.now().plusSeconds(3600), FOREIGN_KEY);
  }

  public static String withIssuer(User user, String issuer) {
    return create(user, issuer, null, Instant.now().plusSeconds(3600), SIGNING_KEY);
  }

  private static String create(
      User user, String issuer, String tenant, Instant expiresAt, KeyPair keys) {
    return custom(
        user,
        spec -> {
          spec.issuer = issuer;
          spec.tenant = tenant;
          spec.expiresAt = expiresAt;
          spec.keys = keys;
        });
  }

  /** Everything a scenario may vary in a token; the defaults are a valid SPID token. */
  public static final class Spec {
    String issuer = "SPID";
    String tenant;
    Instant expiresAt = Instant.now().plusSeconds(3600);
    Instant issuedAt = Instant.now();
    Instant notBefore;
    boolean uid = true;
    String uidValue;
    boolean fiscalNumber = true;
    boolean unsigned;
    KeyPair keys = SIGNING_KEY;

    public Spec issuer(String value) {
      this.issuer = value;
      return this;
    }

    public Spec tenant(String value) {
      this.tenant = value;
      return this;
    }

    public Spec noExpiration() {
      this.expiresAt = null;
      return this;
    }

    public Spec noIssuedAt() {
      this.issuedAt = null;
      return this;
    }

    public Spec notBefore(Instant value) {
      this.notBefore = value;
      return this;
    }

    public Spec noUid() {
      this.uid = false;
      return this;
    }

    public Spec uid(String value) {
      this.uidValue = value;
      return this;
    }

    public Spec noFiscalNumber() {
      this.fiscalNumber = false;
      return this;
    }

    /** {@code alg: none}, no signature at all. */
    public Spec unsigned() {
      this.unsigned = true;
      return this;
    }
  }

  /** Token for {@code user} with the variation described by {@code customizer}. */
  public static String custom(User user, java.util.function.Consumer<Spec> customizer) {
    Spec spec = new Spec();
    customizer.accept(spec);
    JWTCreator.Builder builder =
        JWT.create()
            .withClaim("name", user.name)
            .withClaim("family_name", user.familyName)
            .withClaim("email", user.email);
    if (spec.issuer != null) {
      builder.withIssuer(spec.issuer);
    }
    if (spec.issuedAt != null) {
      builder.withIssuedAt(Date.from(spec.issuedAt));
    }
    if (spec.expiresAt != null) {
      builder.withExpiresAt(Date.from(spec.expiresAt));
    }
    if (spec.notBefore != null) {
      builder.withNotBefore(Date.from(spec.notBefore));
    }
    if (spec.uid) {
      builder.withClaim("uid", spec.uidValue != null ? spec.uidValue : user.uid);
    }
    if (spec.fiscalNumber) {
      builder.withClaim("fiscal_number", user.fiscalNumber);
    }
    if (spec.tenant != null) {
      builder.withClaim("tenant_id", spec.tenant);
    }
    return builder.sign(
        spec.unsigned
            ? Algorithm.none()
            : Algorithm.RSA256(
                (RSAPublicKey) spec.keys.getPublic(), (RSAPrivateKey) spec.keys.getPrivate()));
  }
}
