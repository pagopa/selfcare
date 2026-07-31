package it.pagopa.selfcare.user.event.model;

import it.pagopa.selfcare.user.model.UserGroupNotificationToSend;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TenantAwareUserGroupNotificationToSend extends UserGroupNotificationToSend {
    private String tenantId;
}
