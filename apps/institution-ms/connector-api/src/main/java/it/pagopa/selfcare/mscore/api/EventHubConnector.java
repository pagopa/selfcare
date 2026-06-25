package it.pagopa.selfcare.mscore.api;

import it.pagopa.selfcare.mscore.model.DelegationNotificationToSend;

public interface EventHubConnector {

    boolean sendEvent(DelegationNotificationToSend notification);

}
