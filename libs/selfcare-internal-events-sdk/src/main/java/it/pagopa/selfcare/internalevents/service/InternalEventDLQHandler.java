package it.pagopa.selfcare.internalevents.service;

import it.pagopa.selfcare.internalevents.model.InternalEvent;

import java.util.function.Consumer;

public interface InternalEventDLQHandler {

  boolean handle(int maxMessages, int timeoutSeconds, Consumer<InternalEvent> eventHandler);

  void close();

}
