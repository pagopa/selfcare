package it.pagopa.selfcare.user.model;


import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder(toBuilder = true)
public class TrackEventInput {

    private String documentKey;
    private String userId;
    private String productId;
    private String institutionId;
    private String exception;
    private String productRole;

    public static TrackEventInput toTrackEventInput(UserNotificationToSend userNotificationToSend) {
        return TrackEventInput.builder()
                .documentKey(userNotificationToSend.getId())
                .userId(Optional.ofNullable(userNotificationToSend.getUser()).map(UserToNotify::getUserId).orElse(null))
                .institutionId(userNotificationToSend.getInstitutionId())
                .productId(userNotificationToSend.getProductId())
                .productRole(Optional.ofNullable(userNotificationToSend.getUser()).map(UserToNotify::getProductRole).orElse(null))
                .build();
    }
}
