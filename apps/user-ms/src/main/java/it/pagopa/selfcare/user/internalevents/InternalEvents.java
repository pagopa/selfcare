package it.pagopa.selfcare.user.internalevents;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.internalevents.model.InstitutionUpdatedEvent;
import it.pagopa.selfcare.internalevents.service.InternalEventReceiver;
import it.pagopa.selfcare.internalevents.service.impl.AzureServiceBusEventReceiver;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.user.conf.TenantNotificationConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Startup
@ApplicationScoped
@Slf4j
public class InternalEvents {
  @Inject TenantRegistry registry;
  @Inject TenantNotificationConfig config;
  @Inject InstitutionUpdatedEventHandler handler;
  private final List<InternalEventReceiver> receivers = new ArrayList<>();

  @PostConstruct
  public void init() {
    try {
      for (String tenant : registry.supportedTenantIds()) {
        var settings = config.forTenant(tenant).internalEvents();
        if (settings.enabled()) {
          InternalEventReceiver receiver = createReceiver(settings);
          receivers.add(receiver);
          receiver.subscribe(InstitutionUpdatedEvent.class, event -> handler.handle(tenant, event));
        }
      }
    } catch (RuntimeException failure) {
      destroy();
      throw failure;
    }
  }

  protected InternalEventReceiver createReceiver(TenantNotificationConfig.InternalEventsSettings settings) {
    return settings.connectionString().isBlank()
        ? new AzureServiceBusEventReceiver(settings.namespace(), settings.clientId(), "institution-events", "user-sub", 10, 1)
        : new AzureServiceBusEventReceiver(settings.connectionString(), "institution-events", "user-sub", 10, 1);
  }

  @PreDestroy
  public void destroy() {
    for (InternalEventReceiver receiver : receivers) {
      try {
        receiver.close();
      } catch (RuntimeException failure) {
        log.error("Failed to close internal event receiver", failure);
      }
    }
    receivers.clear();
  }
}
