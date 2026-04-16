package it.pagopa.selfcare.mscore.core;

import java.time.OffsetDateTime;

public interface EventsService {

    void sendDelegationEvents(OffsetDateTime fromDate);

}
