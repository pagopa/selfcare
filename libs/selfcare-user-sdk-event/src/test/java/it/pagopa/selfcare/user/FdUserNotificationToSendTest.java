package it.pagopa.selfcare.user;

import it.pagopa.selfcare.user.model.FdUserNotificationToSend;
import it.pagopa.selfcare.user.model.NotificationUserType;
import it.pagopa.selfcare.user.model.UserToNotify;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.OffsetDateTime;

class FdUserNotificationToSendTest {

    @Test
    void fdUserNotificationToSend_ACTIVE() {
        FdUserNotificationToSend notification = new FdUserNotificationToSend();
        notification.setId("123");
        notification.setInstitutionId("inst-456");
        notification.setProduct("product-789");
        notification.setCreatedAt(OffsetDateTime.now().minusDays(1));
        notification.setUpdatedAt(OffsetDateTime.now());
        notification.setOnboardingTokenId("token-101112");
        notification.setType(NotificationUserType.ACTIVE_USER);
        UserToNotify user = new UserToNotify();
        notification.setUser(user);

        assertEquals("123", notification.getId());
        assertEquals("inst-456", notification.getInstitutionId());
        assertEquals("product-789", notification.getProduct());
        assertNotNull(notification.getCreatedAt());
        assertNotNull(notification.getUpdatedAt());
        assertEquals("token-101112", notification.getOnboardingTokenId());
        assertEquals(NotificationUserType.ACTIVE_USER, notification.getType());
        assertEquals(user, notification.getUser());
    }

    @Test
    void fdUserNotificationToSend_DELETE() {
        FdUserNotificationToSend notification = new FdUserNotificationToSend();
        notification.setId("123");
        notification.setInstitutionId("inst-456");
        notification.setProduct("product-789");
        notification.setCreatedAt(OffsetDateTime.now().minusDays(1));
        notification.setUpdatedAt(OffsetDateTime.now());
        notification.setOnboardingTokenId("token-101112");
        notification.setType(NotificationUserType.DELETE_USER);
        UserToNotify user = new UserToNotify();
        notification.setUser(user);

        assertEquals("123", notification.getId());
        assertEquals("inst-456", notification.getInstitutionId());
        assertEquals("product-789", notification.getProduct());
        assertNotNull(notification.getCreatedAt());
        assertNotNull(notification.getUpdatedAt());
        assertEquals("token-101112", notification.getOnboardingTokenId());
        assertEquals(NotificationUserType.DELETE_USER, notification.getType());
        assertEquals(user, notification.getUser());
    }

    @Test
    void fdUserNotificationToSend_SUSPEND() {
        FdUserNotificationToSend notification = new FdUserNotificationToSend();
        notification.setId("123");
        notification.setInstitutionId("inst-456");
        notification.setProduct("product-789");
        notification.setCreatedAt(OffsetDateTime.now().minusDays(1));
        notification.setUpdatedAt(OffsetDateTime.now());
        notification.setOnboardingTokenId("token-101112");
        notification.setType(NotificationUserType.SUSPEND_USER);
        UserToNotify user = new UserToNotify();
        notification.setUser(user);

        assertEquals("123", notification.getId());
        assertEquals("inst-456", notification.getInstitutionId());
        assertEquals("product-789", notification.getProduct());
        assertNotNull(notification.getCreatedAt());
        assertNotNull(notification.getUpdatedAt());
        assertEquals("token-101112", notification.getOnboardingTokenId());
        assertEquals(NotificationUserType.SUSPEND_USER, notification.getType());
        assertEquals(user, notification.getUser());
    }

    @Test
    void fdUserNotificationToSend_withNullFields_shouldHandleNullValues() {
        FdUserNotificationToSend notification = new FdUserNotificationToSend();

        assertNull(notification.getId());
        assertNull(notification.getInstitutionId());
        assertNull(notification.getProduct());
        assertNull(notification.getCreatedAt());
        assertNull(notification.getUpdatedAt());
        assertNull(notification.getOnboardingTokenId());
        assertNull(notification.getType());
        assertNull(notification.getUser());
    }

    @Test
    void fdUserNotificationToSend_withPartialData_shouldSetFieldsCorrectly() {
        FdUserNotificationToSend notification = new FdUserNotificationToSend();
        notification.setId("123");
        notification.setProduct("product-789");

        assertEquals("123", notification.getId());
        assertNull(notification.getInstitutionId());
        assertEquals("product-789", notification.getProduct());
        assertNull(notification.getCreatedAt());
        assertNull(notification.getUpdatedAt());
        assertNull(notification.getOnboardingTokenId());
        assertNull(notification.getType());
        assertNull(notification.getUser());
    }
}
