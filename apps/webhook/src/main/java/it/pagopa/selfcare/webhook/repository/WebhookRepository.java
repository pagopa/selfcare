package it.pagopa.selfcare.webhook.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.Webhook;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

@ApplicationScoped
public class WebhookRepository implements ReactivePanacheMongoRepository<Webhook> {

  public Uni<List<Webhook>> findActiveWebhooks() {
    return list("status", Webhook.WebhookStatus.ACTIVE);
  }

  public Uni<List<Webhook>> findWebhooks(String tenantId, int page, int size) {
    if (tenantId == null) {
      return findAll().page(page, size).list();
    }

    return find("tenantId", Sort.descending("createdAt"), tenantId).page(page, size).list();
  }

  /**
   * Returns the active webhooks of a tenant that are subscribed to the given product, either
   * explicitly or through the {@link Webhook#ALL_PRODUCTS} wildcard.
   */
  public Uni<List<Webhook>> findActiveWebhooksByProduct(String productId, String tenantId) {
    Document query =
        new Document()
            .append("status", Webhook.WebhookStatus.ACTIVE)
            .append(
                "products", new Document("$in", List.of(productId, Webhook.ALL_PRODUCTS)))
            .append("tenantId", tenantId);
    return find(query).list();
  }

  public Uni<Webhook> findWebhookByProduct(String productId, String tenantId) {
    Document query = new Document().append("productId", productId).append("tenantId", tenantId);
    return find(query).firstResult();
  }

  public Uni<Webhook> findByIdOptional(String id) {
    return findById(new ObjectId(id));
  }

  public Uni<Boolean> deleteByIdSafe(String id) {
    return deleteById(new ObjectId(id));
  }
}
