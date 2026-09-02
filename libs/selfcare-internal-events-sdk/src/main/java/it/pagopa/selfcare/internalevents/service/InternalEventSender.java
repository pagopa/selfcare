package it.pagopa.selfcare.internalevents.service;

import it.pagopa.selfcare.internalevents.model.InternalEvent;

public interface InternalEventSender {

  boolean publish(InternalEvent event);

  void close();

}
