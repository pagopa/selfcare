package it.pagopa.selfcare.webhook.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.WebhookNotificationAttempt;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.bson.types.ObjectId;

@ApplicationScoped
public class WebhookNotificationAttemptRepository
    implements ReactivePanacheMongoRepository<WebhookNotificationAttempt> {

  /** Full delivery history for a notification, ordered from the first to the last attempt. */
  public Uni<List<WebhookNotificationAttempt>> findByNotificationId(ObjectId notificationId) {
    return find("notificationId", Sort.ascending("attemptNumber"), notificationId).list();
  }
}
