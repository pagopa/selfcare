package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.selfcare.onboarding.TestUtils.getMockedContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@QuarkusTest
class NotificationEventResenderServiceDefaultTest {
    @Inject
    NotificationEventResenderServiceDefault notificationEventResenderServiceDefault;

    @InjectMock
    NotificationEventService notificationEventService;

    @InjectMock
    OnboardingService onboardingService;

    @Test
    void resendNotifications_withDoubleOnboardingAndThreeEventsExpected() {
        /* Test that the resendNotifications method works correctly when there are two onboardings 1 COMPLETED (which should produce one notification) and 1 DELETED (which should produce two notifications) and no range is specified */
        // Arrange
        ResendNotificationsFilters filters = ResendNotificationsFilters.builder().onboardingId("test").build();
        ExecutionContext context = getMockedContext();

        Onboarding onboarding = new Onboarding();
        onboarding.setId("id1");
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setActivatedAt(LocalDateTime.of(2023, 2, 1, 0, 0));
        Onboarding onboarding2 = new Onboarding();
        onboarding2.setId("id2");
        onboarding2.setStatus(OnboardingStatus.DELETED);
        onboarding2.setActivatedAt(LocalDateTime.of(2023, 2, 1, 0, 0));
        onboarding2.setDeletedAt(LocalDateTime.of(2023, 2, 1, 0, 0));


        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(List.of(onboarding, onboarding2));
        doNothing().when(notificationEventService).send(any(), any(), any(), any());

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(3)).send(any(), any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNull(resendNotificationsFilters);
    }

    @Test
    void resendNotifications_withDoubleOnboardingAndTwoEventsExpected() {
        /* Test that the resendNotifications method works correctly when there are two onboardings in status COMPLETED (which should produce one notification) and no range is specified */
        // Arrange
        ResendNotificationsFilters filters = ResendNotificationsFilters.builder().onboardingId("test").build();
        ExecutionContext context = getMockedContext();

        Onboarding onboarding = new Onboarding();
        onboarding.setId("id1");
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setActivatedAt(LocalDateTime.of(2023, 2, 1, 0, 0));
        Onboarding onboarding2 = new Onboarding();
        onboarding2.setId("id2");
        onboarding2.setStatus(OnboardingStatus.COMPLETED);
        onboarding2.setActivatedAt(LocalDateTime.of(2023, 2, 1, 0, 0));


        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(List.of(onboarding, onboarding2));
        doNothing().when(notificationEventService).send(any(), any(), any(), any());

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(2)).send(any(), any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNull(resendNotificationsFilters);
    }

    @Test
    void resendNotifications_withOneOnboardingAndTwoEventsExpected() {
        /* Test that the resendNotifications method works correctly when there is 1 onboarding in status DELETED (which should produce two notification) and both activatedAt and deletedAt dates fall in specified range */
        // Arrange
        ResendNotificationsFilters filters = ResendNotificationsFilters.builder()
                .onboardingId("test")
                .from("2023-02-01")
                .build();
        ExecutionContext context = getMockedContext();

        Onboarding onboarding = new Onboarding();
        onboarding.setId("id1");
        onboarding.setStatus(OnboardingStatus.DELETED);
        onboarding.setActivatedAt(LocalDateTime.of(2023, 2, 1, 0, 0));
        onboarding.setDeletedAt(LocalDateTime.of(2023, 2, 3, 0, 0));


        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(List.of(onboarding));
        doNothing().when(notificationEventService).send(any(), any(), any(), any());

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(2)).send(any(), any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNull(resendNotificationsFilters);
    }

    @Test
    void resendNotificationsDoesntStopWhenSendProcessFails() {
        // Arrange
        ResendNotificationsFilters filters = new ResendNotificationsFilters();
        ExecutionContext context = getMockedContext();

        Onboarding onboarding = new Onboarding();
        onboarding.setId("id1");
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setActivatedAt(LocalDateTime.of(2023, 2, 1, 0, 0));
        Onboarding onboarding2 = new Onboarding();
        onboarding2.setId("id2");
        onboarding2.setStatus(OnboardingStatus.COMPLETED);
        onboarding2.setActivatedAt(LocalDateTime.of(2023, 2, 1, 0, 0));

        doThrow(new NotificationException("Error")).when(notificationEventService).send(context, onboarding, QueueEvent.ADD, null);
        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(List.of(onboarding, onboarding2));

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(2)).send(any(), any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNull(resendNotificationsFilters);
    }

    @Test
    void resendNotificationsEndsIncrementingPageIfIsWorkingOnIntermediatePage() {
        // Arrange
        ResendNotificationsFilters filters = ResendNotificationsFilters.builder().onboardingId("test").build();
        ExecutionContext context = getMockedContext();

        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(getMockedList(100));
        doNothing().when(notificationEventService).send(any(), any(), any(), any());

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(100)).send(any(), any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNotNull(resendNotificationsFilters);
        assertEquals(1, resendNotificationsFilters.getPage());
    }

    @Test
    void resendNotificationsEndsWithMoreElementsToRetrieve() {
        // Arrange
        ResendNotificationsFilters filters = new ResendNotificationsFilters();
        ExecutionContext context = getMockedContext();

        doNothing().when(notificationEventService).send(any(), any(), any(), any());
        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(getMockedList(100));

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(100)).send(any(), any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNotNull(resendNotificationsFilters);
        assertEquals(1, resendNotificationsFilters.getPage());
    }

    private List<Onboarding> getMockedList(int i) {
        List<Onboarding> onboardings = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            Onboarding onboarding = new Onboarding();
            onboarding.setId("id" + j);
            onboarding.setStatus(OnboardingStatus.COMPLETED);
            onboarding.setActivatedAt(LocalDateTime.of(2023, 2, 1, 0, 0));
            onboardings.add(onboarding);
        }

        return onboardings;
    }

    @Test
    void resendNotifications_withUserNotification() {
        /* Test that the resendNotifications method works correctly when there are two onboardings 1 COMPLETED (which should produce one notification) and 1 DELETED (which should produce two notifications) and no range is specified */
        // Arrange
        ResendNotificationsFilters filters = ResendNotificationsFilters.builder().onboardingId("test").build();
        ExecutionContext context = getMockedContext();

        Onboarding onboarding = new Onboarding();
        onboarding.setId("id1");
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setProductId("prod-fd");
        onboarding.setActivatedAt(LocalDateTime.of(2023, 2, 1, 0, 0));


        when(onboardingService.getOnboardingsToResend(filters, 0, 100)).thenReturn(List.of(onboarding));
        doNothing().when(notificationEventService).send(any(), any(), any(), any());

        // Act
        ResendNotificationsFilters resendNotificationsFilters = notificationEventResenderServiceDefault.resendNotifications(filters, context);

        // Assert
        verify(notificationEventService, times(1)).send(any(), any(), any(), any());
        verify(onboardingService).getOnboardingsToResend(filters, 0, 100);
        assertNull(resendNotificationsFilters);
    }
}