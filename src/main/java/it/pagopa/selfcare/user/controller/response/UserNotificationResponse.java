package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.user.constant.QueueEvent;
import it.pagopa.selfcare.user.model.notification.UserToNotify;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * This objects wrap user's info sent on topic sc-users
 */
@Data
public class UserNotificationResponse {

    private String id;
    private String institutionId;
    private String productId;
    private String onboardingTokenId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private QueueEvent eventType;
    private UserToNotify user;
}
