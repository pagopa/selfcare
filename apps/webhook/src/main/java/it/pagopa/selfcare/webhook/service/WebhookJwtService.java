package it.pagopa.selfcare.webhook.service;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.util.Pkcs8Utils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Startup
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
   *
   * <p>The parsing is done synchronously on purpose: this bean is first used from a Vert.x
   * event-loop thread (the Storage Queue consumer dispatches delivery via {@code runOnContext}),
   * and blocking there — e.g. {@code .await().indefinitely()} on the reactive variant — throws
   * {@code IllegalStateException: The current thread cannot be blocked}, which would make every
   * webhook delivery fail. {@code @Startup} additionally forces initialization at boot, so a
   * malformed key fails fast instead of silently breaking deliveries later.
   */
  @PostConstruct
  void init() {
    try {
      this.privateKey = Pkcs8Utils.parseRSAPrivateKeyFromPem(privateKeyPem);
    } catch (GeneralSecurityException | RuntimeException e) {
      throw new IllegalStateException(
          "Unable to parse the webhook JWT private key (webhook.jwt.private-key)", e);
    }
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
