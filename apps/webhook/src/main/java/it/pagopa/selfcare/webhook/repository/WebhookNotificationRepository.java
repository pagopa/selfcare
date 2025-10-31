package it.pagopa.selfcare.webhook.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class WebhookNotificationRepository implements ReactivePanacheMongoRepository<WebhookNotification> {
    
    public Uni<List<WebhookNotification>> findPendingNotifications() {
        return list("status in (?1, ?2)", 
                WebhookNotification.NotificationStatus.PENDING, 
                WebhookNotification.NotificationStatus.RETRY);
    }
    
    public Uni<List<WebhookNotification>> findByWebhookId(String webhookId) {
        return list("webhookId", new ObjectId(webhookId));
    }
}
