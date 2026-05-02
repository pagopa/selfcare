package it.pagopa.selfcare.user;

import it.pagopa.selfcare.user.model.UserGroupNotificationToSend;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Set;

class UserGroupNotificationToSendTest {

    @Test
    void userGroupNotificationToSend_withValidData_shouldMapFieldsCorrectly() {
        UserGroupNotificationToSend notification = new UserGroupNotificationToSend();
        notification.setId("id");
        notification.setInstitutionId("institutionId");
        notification.setProductId("productId");
        notification.setName("name");
        notification.setDescription("description");
        notification.setStatus("status");
        notification.setMembers(Set.of("member1", "member2"));
        notification.setCreatedAt(Instant.now());
        notification.setCreatedBy("createdBy");
        notification.setModifiedAt(Instant.now());
        notification.setModifiedBy("modifiedBy");

        assertEquals("id", notification.getId());
        assertEquals("institutionId", notification.getInstitutionId());
        assertEquals("productId", notification.getProductId());
        assertEquals("name", notification.getName());
        assertEquals("description", notification.getDescription());
        assertEquals("status", notification.getStatus());
        assertEquals(Set.of("member1", "member2"), notification.getMembers());
        assertNotNull(notification.getCreatedAt());
        assertEquals("createdBy", notification.getCreatedBy());
        assertNotNull(notification.getModifiedAt());
        assertEquals("modifiedBy", notification.getModifiedBy());
    }

}
