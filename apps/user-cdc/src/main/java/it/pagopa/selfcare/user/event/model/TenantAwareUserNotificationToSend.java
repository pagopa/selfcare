package it.pagopa.selfcare.user.event.model;

import it.pagopa.selfcare.user.model.UserNotificationToSend;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Carries the tenant discriminator on the outbound notification so downstream consumers can route
 * the event without re-reading the source collection. The base payload is owned by
 * selfcare-user-sdk-model, therefore the field is added here instead of in the shared contract.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantAwareUserNotificationToSend extends UserNotificationToSend {
    private String tenantId;
}
