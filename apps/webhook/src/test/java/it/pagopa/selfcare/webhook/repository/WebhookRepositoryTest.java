package it.pagopa.selfcare.webhook.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.webhook.entity.Webhook;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class WebhookRepositoryTest {

  private static final String SELC = "SELC";
  private static final String PNPG = "PNPG";
  private static final String PRODUCT_ID = "prod-io";

  @Inject WebhookRepository webhookRepository;

  @BeforeEach
  void setUp() {
    webhookRepository.deleteAll().await().indefinitely();
  }

  @Test
  void findActiveWebhooks_shouldReturnOnlyActiveWebhooks() {
    // given
    persistWebhook(SELC, PRODUCT_ID, Webhook.WebhookStatus.ACTIVE, LocalDateTime.now());
    persistWebhook(SELC, PRODUCT_ID, Webhook.WebhookStatus.INACTIVE, LocalDateTime.now());

    // when
    List<Webhook> webhooks = webhookRepository.findActiveWebhooks().await().indefinitely();

    // then
    assertEquals(1, webhooks.size());
    assertEquals(Webhook.WebhookStatus.ACTIVE, webhooks.get(0).getStatus());
  }

  @Test
  void findWebhooks_shouldFilterByTenantAndApplyPagination() {
    // given
    Webhook oldest =
        persistWebhook(
            SELC, PRODUCT_ID, Webhook.WebhookStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 10, 0));
    Webhook newest =
        persistWebhook(
            SELC, PRODUCT_ID, Webhook.WebhookStatus.ACTIVE, LocalDateTime.of(2026, 1, 2, 10, 0));
    persistWebhook(PNPG, PRODUCT_ID, Webhook.WebhookStatus.ACTIVE, LocalDateTime.now());

    // when
    List<Webhook> tenantWebhooks = webhookRepository.findWebhooks(SELC, 0, 1).await().indefinitely();
    List<Webhook> allWebhooks = webhookRepository.findWebhooks(null, 0, 10).await().indefinitely();

    // then
    assertEquals(1, tenantWebhooks.size());
    assertEquals(newest.getId(), tenantWebhooks.get(0).getId());
    assertEquals(SELC, tenantWebhooks.get(0).getTenantId());
    assertEquals(3, allWebhooks.size());
    assertTrue(allWebhooks.stream().anyMatch(webhook -> webhook.getId().equals(oldest.getId())));
  }

  @Test
  void findActiveWebhooksByProduct_shouldFilterByProductAndTenant() {
    // given
    Webhook expected =
        persistWebhook(SELC, PRODUCT_ID, Webhook.WebhookStatus.ACTIVE, LocalDateTime.now());
    persistWebhook(PNPG, PRODUCT_ID, Webhook.WebhookStatus.ACTIVE, LocalDateTime.now());
    persistWebhook(SELC, "prod-pagopa", Webhook.WebhookStatus.ACTIVE, LocalDateTime.now());
    persistWebhook(SELC, PRODUCT_ID, Webhook.WebhookStatus.INACTIVE, LocalDateTime.now());

    // when
    List<Webhook> webhooks =
        webhookRepository.findActiveWebhooksByProduct(PRODUCT_ID, SELC).await().indefinitely();

    // then
    assertEquals(1, webhooks.size());
    assertEquals(expected.getId(), webhooks.get(0).getId());
  }

  @Test
  void findWebhookByProduct_shouldReturnWebhookForTenant() {
    // given
    Webhook expected =
        persistWebhook(SELC, PRODUCT_ID, Webhook.WebhookStatus.ACTIVE, LocalDateTime.now());
    persistWebhook(PNPG, PRODUCT_ID, Webhook.WebhookStatus.ACTIVE, LocalDateTime.now());

    // when
    Webhook webhook =
        webhookRepository.findWebhookByProduct(PRODUCT_ID, SELC).await().indefinitely();

    // then
    assertNotNull(webhook);
    assertEquals(expected.getId(), webhook.getId());
  }

  @Test
  void findByIdOptionalAndDeleteByIdSafe_shouldFindAndDeleteWebhook() {
    // given
    Webhook persisted =
        persistWebhook(SELC, PRODUCT_ID, Webhook.WebhookStatus.ACTIVE, LocalDateTime.now());

    // when
    Webhook found = webhookRepository.findByIdOptional(persisted.getId().toHexString()).await().indefinitely();
    boolean deleted =
        webhookRepository.deleteByIdSafe(persisted.getId().toHexString()).await().indefinitely();
    Webhook deletedWebhook =
        webhookRepository.findByIdOptional(persisted.getId().toHexString()).await().indefinitely();

    // then
    assertNotNull(found);
    assertEquals(persisted.getId(), found.getId());
    assertTrue(deleted);
    assertNull(deletedWebhook);
  }

  private Webhook persistWebhook(
      String tenantId,
      String productId,
      Webhook.WebhookStatus status,
      LocalDateTime createdAt) {
    Webhook webhook = new Webhook();
    webhook.setId(new ObjectId());
    webhook.setTenantId(tenantId);
    webhook.setProductId(productId);
    webhook.setProducts(List.of(productId));
    webhook.setStatus(status);
    webhook.setCreatedAt(createdAt);
    webhook.setUrl("https://example.com/webhook");
    webhook.setHttpMethod("POST");
    return webhookRepository.persist(webhook).await().indefinitely();
  }
}
