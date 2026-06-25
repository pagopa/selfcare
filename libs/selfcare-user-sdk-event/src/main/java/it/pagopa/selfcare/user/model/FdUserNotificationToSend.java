package it.pagopa.selfcare.user.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@SuppressWarnings("java:S1068")
public class FdUserNotificationToSend {

    private String id;
    private String institutionId;
    private String product;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String onboardingTokenId;
    private NotificationUserType type;
    private UserToNotify user;

}