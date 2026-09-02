package it.pagopa.selfcare.internalevents.service;

import it.pagopa.selfcare.internalevents.model.InternalEvent;

import java.util.function.Consumer;

public interface InternalEventReceiver {

  <T extends InternalEvent> void subscribe(Class<T> eventType, Consumer<T> eventHandler);

  void close();

}
