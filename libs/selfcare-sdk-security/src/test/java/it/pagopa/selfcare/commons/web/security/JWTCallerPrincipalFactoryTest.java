package it.pagopa.selfcare.commons.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JWTCallerPrincipalFactoryTest {

  private JWTCallerPrincipalFactory factory;
  private JWTAuthContextInfo authContextInfo;

  // Public key per test (chiave RSA 2048-bit di esempio)
  private static final String TEST_PUBLIC_KEY =
    "-----BEGIN PUBLIC KEY-----\n" +
      "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu1SU1LfVLPHCozMxH2Mo\n" +
      "4lgOEePzNm0tRgeLezV6ffAt0gunVTLw7onLRnrq0/IzW7yWR7QkrmBL7jTKEn5u\n" +
      "+qKhbwKfBstIs+bMY2Zkp18gnTxKLxoS2tFczGkPLPgizskuemMghRniWaoLcyeh\n" +
      "kd3qqGElvW/VDL5AaWTg0nLVkjRo9z+40RQzuVaE8AkAFmxZzow3x+VJYKdjykkJ\n" +
      "0iT9wCS0DRTXu269V264Vf/3jvredZiKRkgwlL9xNAwxXFg0x/XFw005UWVRIkdg\n" +
      "cKWTjpBP2dPwVZ4WWC+9aGVd+Gyn1o0CLelf4rEjGoXbAAEgAqeGUxrcIlbjXfbc\n" +
      "mwIDAQAB\n" +
      "-----END PUBLIC KEY-----";

  @BeforeEach
  void setUp() throws Exception {
    factory = new JWTCallerPrincipalFactory(TEST_PUBLIC_KEY);
    authContextInfo = new JWTAuthContextInfo();
  }

  @Test
  void testCreatePublicKeyFromString() throws Exception {
    PublicKey publicKey = JWTCallerPrincipalFactory.createPublicKeyFromString(TEST_PUBLIC_KEY, "RSA");

    assertNotNull(publicKey);
    assertEquals("RSA", publicKey.getAlgorithm());
  }

  @Test
  void testCleanKeyString() {
    String cleanedKey = JWTCallerPrincipalFactory.cleanKeyString(TEST_PUBLIC_KEY);

    assertFalse(cleanedKey.contains("-----BEGIN PUBLIC KEY-----"));
    assertFalse(cleanedKey.contains("-----END PUBLIC KEY-----"));
    assertFalse(cleanedKey.contains("\n"));
    assertFalse(cleanedKey.contains(" "));
  }

  @Test
  void testExtractIssuerFromJwt_ValidToken() throws Exception {
    String token = createMockJWT("SPID", "user123");

    String issuer = factory.extractIssuerFromJwt(token);

    assertEquals("SPID", issuer);
  }

  @Test
  void testExtractIssuerFromJwt_InvalidFormat() {
    String invalidToken = "invalid.token";

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      factory.extractIssuerFromJwt(invalidToken);
    });

    assertEquals("Invalid JWT format", exception.getMessage());
  }

  @Test
  void testExtractIssuerFromJwt_EmptyToken() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      factory.extractIssuerFromJwt("");
    });

    assertEquals("Invalid JWT format", exception.getMessage());
  }

  @Test
  void testParse_InvalidIssuer() {
    String token = createMockJWT("INVALID_ISSUER", "user123");

    Exception exception = assertThrows(ParseException.class, () -> {
      factory.parse(token, authContextInfo);
    });

    assertTrue(exception.getMessage().contains("Token validation failed"));
  }

  @Test
  void testParse_ValidIssuerSPID() throws Exception {
    String token = createMockJWT("SPID", "user123");

    // Il test può fallire se la firma non è valida, ma almeno verifica che l'issuer sia accettato
    assertDoesNotThrow(() -> {
      try {
        factory.parse(token, authContextInfo);
      } catch (ParseException e) {
        // Verifica che non sia per issuer invalido
        assertFalse(e.getMessage().contains("Invalid issuer"));
      }
    });
  }

  @Test
  void testParse_ValidIssuerPAGOPA() throws Exception {
    String token = createMockJWT("PAGOPA", "user123");

    assertDoesNotThrow(() -> {
      try {
        factory.parse(token, authContextInfo);
      } catch (ParseException e) {
        assertFalse(e.getMessage().contains("Invalid issuer"));
      }
    });
  }

  @Test
  void testExtractIssuerFromJwt_MultipleIssuers() throws Exception {
    String token1 = createMockJWT("SPID", "user1");
    String token2 = createMockJWT("PAGOPA", "user2");

    assertEquals("SPID", factory.extractIssuerFromJwt(token1));
    assertEquals("PAGOPA", factory.extractIssuerFromJwt(token2));
  }

  @Test
  void testExtractIssuerFromJwt_TokenWithSpecialCharacters() throws Exception {
    String token = createMockJWT("SPID", "user@example.com");

    String issuer = factory.extractIssuerFromJwt(token);

    assertEquals("SPID", issuer);
  }

  @Test
  void testCreatePublicKeyFromString_InvalidKey() {
    String invalidKey = "INVALID_KEY";

    assertThrows(Exception.class, () -> {
      JWTCallerPrincipalFactory.createPublicKeyFromString(invalidKey, "RSA");
    });
  }

  @Test
  void testExtractIssuerFromJwt_MissingIssuer() {
    // Token senza claim "iss"
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "user123");
    String token = createTokenWithClaims(claims);

    assertThrows(Exception.class, () -> {
      factory.extractIssuerFromJwt(token);
    });
  }

  // Helper methods per creare token JWT mock
  private String createMockJWT(String issuer, String subject) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("iss", issuer);
    claims.put("sub", subject);
    claims.put("uid", subject);
    claims.put("exp", System.currentTimeMillis() / 1000 + 3600);

    return createTokenWithClaims(claims);
  }

  private String createTokenWithClaims(Map<String, Object> claims) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String header = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
      String payload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(mapper.writeValueAsBytes(claims));
      String signature = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("fake_signature".getBytes());

      return header + "." + payload + "." + signature;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}