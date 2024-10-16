package it.pagopa.selfcare.user;

import it.pagopa.selfcare.user.model.TrackEventInput;
import it.pagopa.selfcare.user.model.UserGroupNotificationToSend;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.UserToNotify;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TrackEventInputTest {

    @Test
    void toTrackEventInput_withUserNotification() {
        UserNotificationToSend userNotification = new UserNotificationToSend();
        userNotification.setId("docKey");
        UserToNotify user = new UserToNotify();
        user.setUserId("userId");
        user.setProductRole("productRole");
        userNotification.setUser(user);
        userNotification.setInstitutionId("institutionId");
        userNotification.setProductId("productId");

        TrackEventInput result = TrackEventInput.toTrackEventInput(userNotification);

        assertEquals("docKey", result.getDocumentKey());
        assertEquals("userId", result.getUserId());
        assertEquals("institutionId", result.getInstitutionId());
        assertEquals("productId", result.getProductId());
        assertEquals("productRole", result.getProductRole());
    }

    @Test
    void toTrackEventInputForUserGroup_withUserGroupNotification() {
        UserGroupNotificationToSend userGroupNotification = new UserGroupNotificationToSend();
        userGroupNotification.setId("docKey");
        userGroupNotification.setInstitutionId("institutionId");
        userGroupNotification.setProductId("productId");
        userGroupNotification.setMembers(Set.of("member1", "member2"));

        TrackEventInput result = TrackEventInput.toTrackEventInputForUserGroup(userGroupNotification);

        assertEquals("docKey", result.getDocumentKey());
        assertEquals("institutionId", result.getInstitutionId());
        assertEquals("productId", result.getProductId());
    }
}