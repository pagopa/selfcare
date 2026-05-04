package it.pagopa.selfcare.user.model;

import it.pagopa.selfcare.user.model.constants.QueueEvent;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@SuppressWarnings("java:S1068")
public class UserNotificationToSend {

    private String id;
    private String institutionId;
    private String productId;
    private String onboardingTokenId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private QueueEvent eventType;
    private UserToNotify user;
}
