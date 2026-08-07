package it.pagopa.selfcare.webhook.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.util.Pkcs8Utils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class WebhookJwtService {

  @ConfigProperty(name = "webhook.jwt.private-key")
  String privateKeyPem;

  @ConfigProperty(name = "webhook.jwt.issuer", defaultValue = "PAGOPA")
  String issuer;

  @ConfigProperty(name = "webhook.jwt.audience", defaultValue = "selfcare-webhook")
  String audience;

  @ConfigProperty(name = "webhook.jwt.duration-minutes", defaultValue = "5")
  long durationMinutes;

  private PrivateKey privateKey;

  /**
   * The PEM-encoded private key never changes at runtime, but parsing/decoding it involves Base64
   * decoding and RSA key material generation on every call. Parse it once when the bean is
   * constructed instead of on every notification delivery/retry.
   */
  @PostConstruct
  void init() {
    this.privateKey = Pkcs8Utils.extractRSAPrivateKeyFromPem(privateKeyPem).await().indefinitely();
  }

  public Uni<String> generateNotificationToken(Webhook webhook, WebhookNotification notification) {
    Instant now = Instant.now();
    return Uni.createFrom()
        .item(
            () ->
                Jwt.claims()
                    .issuer(issuer)
                    .audience(audience)
                    .subject(webhook.getProductId())
                    .issuedAt(now)
                    .expiresAt(now.plus(Duration.ofMinutes(durationMinutes)))
                    .claim("jti", notification.getId().toString())
                    .claim("product_id", webhook.getProductId())
                    .claim("webhook_id", webhook.getId().toString())
                    .claim("tenant_id", webhook.getTenantId())
                    .claim("topic", notification.getTopic())
                    .claim("notification_id", notification.getId().toString())
                    .sign(privateKey));
  }
}
