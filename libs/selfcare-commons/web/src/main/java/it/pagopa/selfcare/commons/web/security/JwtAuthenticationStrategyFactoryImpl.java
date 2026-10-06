package it.pagopa.selfcare.commons.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Concrete Factory of {@link JwtAuthenticationStrategy}
 */
@Slf4j
@Service
public class JwtAuthenticationStrategyFactoryImpl implements JwtAuthenticationStrategyFactory {

  private final BeanFactory beanFactory;

  @Autowired
  public JwtAuthenticationStrategyFactoryImpl(final BeanFactory beanFactory) {
    log.trace("Initializing {}", JwtAuthenticationStrategyFactoryImpl.class.getSimpleName());
    this.beanFactory = beanFactory;
  }

  @Override
  public JwtAuthenticationStrategy create(final String jwt) {
    final JwtAuthenticationStrategy bean;
    final String issuer;
    try {

      int signatureSeparator = jwt == null ? -1 : jwt.lastIndexOf('.');
      String unsignedJwt = signatureSeparator < 0
          ? jwt
          : jwt.substring(0, signatureSeparator + 1);
      Claims claims = Jwts.parserBuilder()
          .build()
          .parseClaimsJwt(unsignedJwt)
          .getBody();
      issuer = claims.getIssuer();

    } catch (JwtException e) {
      throw new JwtAuthenticationException(e.getMessage());
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
