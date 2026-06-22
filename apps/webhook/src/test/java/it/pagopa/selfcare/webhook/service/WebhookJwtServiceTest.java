package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookJwtServiceTest {

  @Inject WebhookJwtService webhookJwtService;

  @Inject JWTParser jwtParser;

  @Test
  void generateNotificationToken_shouldIncludeWebhookClaims() throws Exception {
    ObjectId webhookId = new ObjectId();
    ObjectId notificationId = new ObjectId();
    Webhook webhook = new Webhook();
    webhook.setId(webhookId);
    webhook.setProductId("prod-test");

    WebhookNotification notification = new WebhookNotification();
    notification.setId(notificationId);

    String token =
        webhookJwtService
            .generateNotificationToken(webhook, notification)
            .await()
            .indefinitely();

    JsonWebToken parsedToken = jwtParser.parseOnly(token);

    assertNotNull(token);
    assertEquals("PAGOPA", parsedToken.getIssuer());
    assertEquals("prod-test", parsedToken.getSubject());
    assertEquals("prod-test", parsedToken.getClaim("product_id"));
    assertEquals(webhookId.toString(), parsedToken.getClaim("webhook_id"));
    assertEquals(notificationId.toString(), parsedToken.getClaim("notification_id"));
  }
}
