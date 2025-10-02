package it.pagopa.selfcare.commons.web.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.jwt.auth.principal.*;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Set;

@ApplicationScoped
@Alternative
@Priority(1)
public class JWTCallerPrincipalFactory extends DefaultJWTCallerPrincipalFactory {

  private final Set<String> validIssuers = Set.of("SPID", "PAGOPA");
  private final PublicKey sharedPublicKey;

  @Inject
  public JWTCallerPrincipalFactory(@ConfigProperty(name = "mp.jwt.verify.publickey") String pubKey) throws Exception {
    sharedPublicKey = createPublicKeyFromString(pubKey, "RSA");
  }

  @Override
  public JWTCallerPrincipal parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException {
    try {
      String issuer = extractIssuerFromJwt(token);
      if (!validIssuers.contains(issuer)) {
        throw new ParseException("Invalid issuer: " + issuer);
      }

      JWTAuthContextInfo contextInfo = new JWTAuthContextInfo(authContextInfo);
      contextInfo.setPublicVerificationKey(sharedPublicKey);
      contextInfo.setIssuedBy(issuer);
      contextInfo.setRequiredClaims(Set.of("uid"));
      contextInfo.setDefaultSubjectClaim("uid");

      return super.parse(token, contextInfo);

    } catch (Exception e) {
      throw new ParseException("Token validation failed");
    }
  }

  public static PublicKey createPublicKeyFromString(String publicKeyString, String algorithm) throws Exception {
    String publicKeyPEM = cleanKeyString(publicKeyString);
    byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
    return keyFactory.generatePublic(keySpec);
  }

  public static String cleanKeyString(String publicKeyString) {
    return publicKeyString
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replace("-----END PUBLIC KEY-----", "")
      .replaceAll("\\s", "");
  }

  public String extractIssuerFromJwt(String token) throws Exception {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid JWT format");
    }

    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
    ObjectMapper mapper = new ObjectMapper();
    JsonNode claims = mapper.readTree(payload);
    return claims.get("iss").asText();
  }
}
