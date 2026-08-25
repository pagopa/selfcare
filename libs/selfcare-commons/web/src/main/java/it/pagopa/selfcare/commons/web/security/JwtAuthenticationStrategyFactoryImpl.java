package it.pagopa.selfcare.commons.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Concrete Factory of {@link JwtAuthenticationStrategy}
 */
@Slf4j
@Service
public class JwtAuthenticationStrategyFactoryImpl implements JwtAuthenticationStrategyFactory {

  private final BeanFactory beanFactory;

  @Value("${jwt.signingKey}")
  private String signingKey;

  @Autowired
  private JwtService jwtService;

  @Autowired
  public JwtAuthenticationStrategyFactoryImpl(final BeanFactory beanFactory) {
    log.trace("Initializing {}", JwtAuthenticationStrategyFactoryImpl.class.getSimpleName());
    this.beanFactory = beanFactory;
  }

  private PublicKey getPublicKey(String signingKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    log.trace("getPublicKey");
    String publicKeyPEM = signingKey
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replaceAll(System.lineSeparator(), "")
      .replace("-----END PUBLIC KEY-----", "");

    byte[] encoded = Base64.getMimeDecoder().decode(publicKeyPEM);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
    return keyFactory.generatePublic(keySpec);
  }


  @Override
  public JwtAuthenticationStrategy create(final String jwt) {
    final JwtAuthenticationStrategy bean;
    final String issuer;
    try {

      if (Jwts.parserBuilder().build().isSigned(jwt)) {
        Claims claims = Jwts.parserBuilder()
          .setSigningKey(getPublicKey(signingKey))
          .build()
          .parseClaimsJws(jwt)
          .getBody();

        issuer = claims.getIssuer();
      } else {
        Claims claims = Jwts.parserBuilder()
          .build()
          .parseClaimsJwt(jwt)
          .getBody();

        issuer = claims.getIssuer();
      }

    } catch (JwtException e) {
      throw new JwtAuthenticationException(e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
    bean = switch (issuer) {
      case "SPID" -> beanFactory.getBean(SpidJwtAuthenticationStrategy.class);
      case "kubernetes/serviceaccount" ->
        beanFactory.getBean(K8sJwtAuthenticationStrategy.class);
      case "PAGOPA" -> beanFactory.getBean(PagopaJwtAuthenticationStrategy.class);
      default -> throw new IllegalArgumentException("Unknown issuer");
    };
    return bean;
  }

}
