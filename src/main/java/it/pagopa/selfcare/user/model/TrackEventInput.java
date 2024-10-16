package it.pagopa.selfcare.user.model;


import lombok.Builder;
import lombok.Data;

import java.util.List;
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
    private List<String> groupMembers;

    public static TrackEventInput toTrackEventInput(UserNotificationToSend userNotificationToSend) {
        return TrackEventInput.builder()
                .documentKey(userNotificationToSend.getId())
                .userId(Optional.ofNullable(userNotificationToSend.getUser()).map(UserToNotify::getUserId).orElse(null))
                .institutionId(userNotificationToSend.getInstitutionId())
                .productId(userNotificationToSend.getProductId())
                .productRole(Optional.ofNullable(userNotificationToSend.getUser()).map(UserToNotify::getProductRole).orElse(null))
                .build();
    }

    public static TrackEventInput toTrackEventInputForUserGroup(UserGroupNotificationToSend userGroupEntity) {
        TrackEventInputBuilder trackEventInputBuilder = TrackEventInput.builder()
                .documentKey(userGroupEntity.getId())
                .institutionId(userGroupEntity.getInstitutionId())
                .productId(userGroupEntity.getProductId());

        if(userGroupEntity.getMembers() != null && !userGroupEntity.getMembers().isEmpty()) {
            trackEventInputBuilder.groupMembers(userGroupEntity.getMembers().stream().toList());
        }
        return trackEventInputBuilder.build();
    }
}
