package it.pagopa.selfcare.webhook.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.mutiny.core.Vertx;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class WebhookNotificationConsumer {

  @Inject WebhookNotificationRepository notificationRepository;
  @Inject WebhookNotificationService notificationService;
  @Inject Vertx vertx;

  @ConfigProperty(name = "webhook.service-bus.enabled", defaultValue = "false")
  boolean enabled;

  @ConfigProperty(name = "webhook.service-bus.namespace")
  String namespace;

  @ConfigProperty(name = "webhook.service-bus.queue")
  String queue;

  @ConfigProperty(name = "webhook.service-bus.connection-string", defaultValue = "none")
  String connectionString;

  @ConfigProperty(name = "webhook.service-bus.max-concurrent-calls", defaultValue = "8")
  int maxConcurrentCalls;

  private ServiceBusProcessorClient processor;

  void start(@Observes StartupEvent event) {
    if (!enabled) {
      return;
    }
    ServiceBusClientBuilder clientBuilder = new ServiceBusClientBuilder();
    if ("none".equals(connectionString)) {
      clientBuilder
          .fullyQualifiedNamespace(namespace)
          .credential(new DefaultAzureCredentialBuilder().build());
    } else {
      clientBuilder.connectionString(connectionString);
    }
    processor =
        clientBuilder
            .processor()
            .queueName(queue)
            .disableAutoComplete()
            .maxConcurrentCalls(maxConcurrentCalls)
            .maxAutoLockRenewDuration(Duration.ofMinutes(5))
            .processMessage(this::processMessage)
            .processError(this::processError)
            .buildProcessorClient();
    processor.start();
  }

  private void processMessage(ServiceBusReceivedMessageContext context) {
    String notificationId = context.getMessage().getBody().toString();
    if (!ObjectId.isValid(notificationId)) {
      log.error("Discarding Service Bus message with invalid notification ID: {}", notificationId);
      context.complete();
      return;
    }

    Context processingContext = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
    VertxContextSafetyToggle.setContextSafe(processingContext, true);
    processingContext.runOnContext(ignored -> processNotification(context, notificationId));
  }

  private void processNotification(
      ServiceBusReceivedMessageContext context, String notificationId) {
    notificationRepository
        .claimForProcessing(notificationId, 5)
        .onItem()
        .transformToUni(
            notification -> {
              if (notification == null) {
                return io.smallrye.mutiny.Uni.createFrom().<WebhookNotification>nullItem();
              }
              return notificationService
                  .processNotification(notification)
                  .call(ignored -> notificationRepository.releaseProcessingLock(notification))
                  .replaceWith(notification);
            })
        .subscribe()
        .with(
            notification -> {
              if (notification != null
                  && notification.getStatus() == WebhookNotification.NotificationStatus.RETRY) {
                context.abandon();
              } else {
                context.complete();
              }
            },
            error -> {
              log.error("Unable to process Service Bus notification {}", notificationId, error);
              context.abandon();
            });
  }

  private void processError(ServiceBusErrorContext context) {
    log.error(
        "Service Bus consumer error: {}",
        context.getException().getMessage(),
        context.getException());
  }

  @PreDestroy
  void stop() {
    if (processor != null) {
      processor.close();
    }
  }
}
