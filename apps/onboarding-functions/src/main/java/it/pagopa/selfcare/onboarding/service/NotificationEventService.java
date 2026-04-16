package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;

public interface NotificationEventService {

    void send(ExecutionContext context, Onboarding onboarding, QueueEvent queueEvent);

    void send(ExecutionContext context, Onboarding onboarding, QueueEvent queueEvent, String notificationEventTraceId);
}
