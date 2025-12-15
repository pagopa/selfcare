package it.pagopa.selfcare.webhook.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.Webhook;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class WebhookRepository implements ReactivePanacheMongoRepository<Webhook> {

  public Uni<List<Webhook>> findActiveWebhooks() {
    return list("status", Webhook.WebhookStatus.ACTIVE);
  }

  public Uni<List<Webhook>> findActiveWebhooksByProduct(String productId) {
    // MongoDB query: { "status": "ACTIVE", "products": { "$in": ["productId"] } }
    Document query = new Document()
      .append("status", Webhook.WebhookStatus.ACTIVE)
      .append("products", productId);
    return find(query).list();
  }

  public Uni<Webhook> findWebhookByProduct(String productId) {
    // MongoDB query: { "productId": "productId" }
    Document query = new Document()
      .append("productId", productId);
    return find(query).firstResult();
  }

  public Uni<Webhook> findByIdOptional(String id) {
    return findById(new ObjectId(id));
  }

  public Uni<Boolean> deleteByIdSafe(String id) {
    return deleteById(new ObjectId(id));
  }
}
