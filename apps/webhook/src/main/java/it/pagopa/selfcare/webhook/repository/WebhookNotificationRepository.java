package it.pagopa.selfcare.webhook.repository;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;

@Slf4j
@ApplicationScoped
public class WebhookNotificationRepository
    implements ReactivePanacheMongoRepository<WebhookNotification> {

  /**
   * Thread-safe method to find and lock pending notifications. Uses MongoDB's findAndModify to
   * atomically claim notifications for processing. This prevents multiple containers from
   * processing the same notification.
   *
   * @param limit Maximum number of notifications to claim
   * @param lockDurationMinutes Duration of the lock in minutes
   * @return List of claimed notifications
   */
  public Uni<List<WebhookNotification>> findAndLockPendingNotifications(
      int limit, int lockDurationMinutes) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime lockUntil = now.plusMinutes(lockDurationMinutes);

    // Query: find notifications that are:
    // - status is PENDING or RETRY
    // - NOT already processing OR processing lock has expired
    Document query =
        new Document()
            .append(
                "status",
                new Document(
                    "$in",
                    List.of(
                        WebhookNotification.NotificationStatus.PENDING.name(),
                        WebhookNotification.NotificationStatus.RETRY.name())))
            .append(
                "$or",
                List.of(
                    new Document("processing", new Document("$ne", true)),
                    new Document("processingUntil", new Document("$lt", now))));

    // Update: mark as processing and set lock expiration
    Document update =
        new Document(
            "$set", new Document().append("processing", true).append("processingUntil", lockUntil));

    FindOneAndUpdateOptions options =
        new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

    // Process multiple documents by repeatedly calling findAndModify
    return Multi.createBy()
        .repeating()
        .uni(() -> mongoCollection().findOneAndUpdate(query, update, options))
        .atMost(limit)
        .filter(Objects::nonNull)
        .map(doc -> mongoCollection().getDocumentClass().cast(doc))
        .collect()
        .asList();
  }

  /** Release the processing lock on a notification */
  public Uni<WebhookNotification> releaseProcessingLock(WebhookNotification notification) {
    log.info("ReleaseProcessingLock {} notifications", notification.getId().toString());
    notification.setProcessing(false);
    notification.setProcessingUntil(null);
    return update(notification);
  }

  public Uni<WebhookNotification> claimForProcessing(
      String notificationId, int lockDurationMinutes) {
    LocalDateTime now = LocalDateTime.now();
    Document query =
        new Document("_id", new ObjectId(notificationId))
            .append(
                "status",
                new Document(
                    "$in",
                    List.of(
                        WebhookNotification.NotificationStatus.PENDING.name(),
                        WebhookNotification.NotificationStatus.RETRY.name())))
            .append(
                "$or",
                List.of(
                    new Document("processing", new Document("$ne", true)),
                    new Document("processingUntil", new Document("$lt", now))));

    Document update =
        new Document(
            "$set",
            new Document("processing", true)
                .append("processingUntil", now.plusMinutes(lockDurationMinutes)));

    return mongoCollection()
        .findOneAndUpdate(
            query, update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
  }

  public Uni<Void> markAsPublished(ObjectId notificationId) {
    return mongoCollection()
        .updateOne(
            Filters.eq("_id", notificationId),
            Updates.combine(
                Updates.set("busPublishedAt", LocalDateTime.now()),
                Updates.set("publishing", false),
                Updates.unset("publishingUntil")))
        .replaceWithVoid();
  }

  public Uni<Void> releasePublishingLock(ObjectId notificationId) {
    return mongoCollection()
        .updateOne(
            Filters.eq("_id", notificationId),
            Updates.combine(Updates.set("publishing", false), Updates.unset("publishingUntil")))
        .replaceWithVoid();
  }

  public Uni<List<WebhookNotification>> claimUnpublishedNotifications(
      int limit, int lockDurationMinutes) {
    LocalDateTime now = LocalDateTime.now();
    Document query =
        new Document("status", WebhookNotification.NotificationStatus.PENDING.name())
            .append("busPublishedAt", null)
            .append(
                "$or",
                List.of(
                    new Document("publishing", new Document("$ne", true)),
                    new Document("publishingUntil", new Document("$lt", now))));
    Document update =
        new Document(
            "$set",
            new Document("publishing", true)
                .append("publishingUntil", now.plusMinutes(lockDurationMinutes)));

    return Multi.createBy()
        .repeating()
        .uni(
            () ->
                mongoCollection()
                    .findOneAndUpdate(
                        query,
                        update,
                        new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)))
        .atMost(limit)
        .filter(Objects::nonNull)
        .map(doc -> mongoCollection().getDocumentClass().cast(doc))
        .collect()
        .asList();
  }

  public Uni<List<WebhookNotification>> findByWebhookId(String webhookId) {
    return list("webhookId", new ObjectId(webhookId));
  }
}
