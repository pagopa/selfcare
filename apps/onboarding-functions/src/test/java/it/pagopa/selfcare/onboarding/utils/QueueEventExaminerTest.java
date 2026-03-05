package it.pagopa.selfcare.onboarding.utils;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class QueueEventExaminerTest {
  @Inject
  private QueueEventExaminer queueEventExaminer;

  private NotificationConfig notificationConfig;

  @BeforeEach
  void setup() {
    notificationConfig = mock(NotificationConfig.class);
    when(notificationConfig.minutesThresholdForUpdateNotification()).thenReturn(5);
  }

  @Test
  @DisplayName("Should return ADD event for COMPLETED status and update within threshold")
  void shouldReturnAddEventForCompletedStatusAndUpdateWithinThreshold() {
    Onboarding onboarding = new Onboarding();
    onboarding.setStatus(OnboardingStatus.COMPLETED);
    onboarding.setUpdatedAt(LocalDateTime.now());
    onboarding.setCreatedAt(LocalDateTime.now().minusMinutes(1));

    QueueEvent result = queueEventExaminer.determineEventType(onboarding);

    assertEquals(QueueEvent.ADD, result);
  }

  @Test
  @DisplayName("Should return UPDATE event for COMPLETED status and update over threshold")
  void shouldReturnUpdateEventForCompletedStatusAndUpdateOverThreshold() {
    Onboarding onboarding = new Onboarding();
    onboarding.setStatus(OnboardingStatus.COMPLETED);
    onboarding.setUpdatedAt(LocalDateTime.now());
    onboarding.setCreatedAt(LocalDateTime.now().minusMinutes(10));

    QueueEvent result = queueEventExaminer.determineEventType(onboarding);

    assertEquals(QueueEvent.UPDATE, result);
  }

  @Test
  @DisplayName("Should return UPDATE event for DELETED status")
  void shouldReturnUpdateEventForDeletedStatus() {
    Onboarding onboarding = new Onboarding();
    onboarding.setStatus(OnboardingStatus.DELETED);

    QueueEvent result = queueEventExaminer.determineEventType(onboarding);

    assertEquals(QueueEvent.UPDATE, result);
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException for unsupported status")
  void shouldThrowIllegalArgumentExceptionForUnsupportedStatus() {
    Onboarding onboarding = new Onboarding();
    onboarding.setStatus(OnboardingStatus.PENDING);

    assertThrows(IllegalArgumentException.class, () -> queueEventExaminer.determineEventType(onboarding));
  }
}