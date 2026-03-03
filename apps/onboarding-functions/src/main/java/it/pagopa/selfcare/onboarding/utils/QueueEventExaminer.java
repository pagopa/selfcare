package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
public class QueueEventExaminer {
  private final int minutesThresholdForUpdateNotification;

  public QueueEventExaminer(NotificationConfig notificationConfig) {
    this.minutesThresholdForUpdateNotification = notificationConfig.minutesThresholdForUpdateNotification();
  }

  public QueueEvent determineEventType(Onboarding onboarding) {
    return switch (onboarding.getStatus()) {
      case COMPLETED -> (isOverUpdateThreshold(onboarding.getUpdatedAt(), onboarding.getCreatedAt())) ? QueueEvent.UPDATE : QueueEvent.ADD;
      case DELETED -> QueueEvent.UPDATE;
      default -> throw new IllegalArgumentException("Onboarding status not supported");
    };
  }

  private boolean isOverUpdateThreshold(LocalDateTime updatedAt, LocalDateTime createdAt) {
    return Objects.nonNull(updatedAt)
      && Objects.nonNull(createdAt)
      && updatedAt.isAfter(createdAt.plusMinutes(minutesThresholdForUpdateNotification));
  }
}
