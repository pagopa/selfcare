package it.pagopa.selfcare.webhook.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.util.Pkcs8Utils;
import jakarta.enterprise.context.ApplicationScoped;
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

  public Uni<String> generateNotificationToken(Webhook webhook, WebhookNotification notification) {
    Instant now = Instant.now();
    return Pkcs8Utils.extractRSAPrivateKeyFromPem(privateKeyPem)
        .map(
            privateKey ->
                Jwt.claims()
                    .issuer(issuer)
                    .audience(audience)
                    .subject(webhook.getProductId())
                    .issuedAt(now)
                    .expiresAt(now.plus(Duration.ofMinutes(durationMinutes)))
                    .claim("jti", notification.getId().toString())
                    .claim("product_id", webhook.getProductId())
                    .claim("webhook_id", webhook.getId().toString())
                    .claim("notification_id", notification.getId().toString())
                    .sign(privateKey));
  }
}
