package it.pagopa.selfcare.user.model;

import it.pagopa.selfcare.user.model.constants.QueueEvent;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserNotificationToSend {

    private String id;
    private String institutionId;
    private String productId;
    private String onboardingTokenId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private QueueEvent eventType;
    private UserToNotify user;
}
