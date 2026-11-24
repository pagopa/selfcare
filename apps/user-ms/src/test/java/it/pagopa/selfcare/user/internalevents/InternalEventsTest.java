package it.pagopa.selfcare.user.internalevents;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.internalevents.model.InstitutionUpdatedEvent;
import it.pagopa.selfcare.internalevents.service.InternalEventReceiver;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.user.conf.TenantNotificationConfig;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InternalEventsTest {
  @Test
  void closesAllReceiversOnceAtShutdown() {
    var ar = mock(InternalEventReceiver.class);
    var pnpg = mock(InternalEventReceiver.class);
    var events = configured(ar, pnpg);
    events.init();
    events.destroy();
    events.destroy();
    verify(ar, times(1)).close();
    verify(pnpg, times(1)).close();
  }

  @Test
  void partialStartupFailureClosesBothStartedAndFailingReceivers() {
    var ar = mock(InternalEventReceiver.class);
    var pnpg = mock(InternalEventReceiver.class);
    doThrow(new IllegalStateException("Subscription unavailable"))
        .when(pnpg).subscribe(eq(InstitutionUpdatedEvent.class), any());
    var events = configured(ar, pnpg);
    assertThrows(IllegalStateException.class, events::init);
    verify(ar).close();
    verify(pnpg).close();
  }

  private InternalEvents configured(InternalEventReceiver ar, InternalEventReceiver pnpg) {
    var receivers = new ArrayDeque<>(List.of(ar, pnpg));
    var events = new InternalEvents() {
      @Override
      protected InternalEventReceiver createReceiver(TenantNotificationConfig.InternalEventsSettings settings) {
        return receivers.removeFirst();
      }
    };
    events.registry = mock(TenantRegistry.class);
    events.config = mock(TenantNotificationConfig.class);
    events.handler = mock(InstitutionUpdatedEventHandler.class);
    when(events.registry.supportedTenantIds())
        .thenReturn(new LinkedHashSet<>(List.of("AR", "PNPG")));
    var settings = new TenantNotificationConfig.Settings(
        "http://localhost", false, "sender", "", Map.of(), false, false,
        new TenantNotificationConfig.InternalEventsSettings(true, "namespace", "client", ""));
    when(events.config.forTenant(any())).thenReturn(settings);
    return events;
  }
}
