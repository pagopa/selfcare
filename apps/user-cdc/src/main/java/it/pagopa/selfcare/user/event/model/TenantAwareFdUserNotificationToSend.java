package it.pagopa.selfcare.user.event.model;

import it.pagopa.selfcare.user.model.FdUserNotificationToSend;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Carries the tenant discriminator on the outbound Fd notification so downstream consumers can route
 * the event without re-reading the source collection. The base payload is owned by
 * selfcare-user-sdk-model, therefore the field is added here instead of in the shared contract.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantAwareFdUserNotificationToSend extends FdUserNotificationToSend {
    private String tenantId;
}
