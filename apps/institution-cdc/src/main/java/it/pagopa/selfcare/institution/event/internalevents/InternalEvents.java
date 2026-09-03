package it.pagopa.selfcare.institution.event.internalevents;

import it.pagopa.selfcare.institution.event.entity.InstitutionEntity;
import it.pagopa.selfcare.internalevents.service.InternalEventSender;
import it.pagopa.selfcare.internalevents.service.impl.AzureServiceBusEventSender;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
@Slf4j
public class InternalEvents {

  private final InternalEventSender institutionEventsSender;
  private final InternalEventsMapper internalEventsMapper;

  public InternalEvents(@ConfigProperty(name = "institution-cdc.internalevents.enabled", defaultValue = "false") boolean internalEventsEnabled,
                        @ConfigProperty(name = "institution-cdc.internalevents.connection-string") Optional<String> internalEventsConnectionString,
                        @ConfigProperty(name = "institution-cdc.internalevents.namespace") Optional<String> internalEventsNamespace,
                        @ConfigProperty(name = "institution-cdc.internalevents.client-id") Optional<String> internalEventsClientId,
                        InternalEventsMapper internalEventsMapper) {
    this.internalEventsMapper = internalEventsMapper;
    if (!internalEventsEnabled) {
      this.institutionEventsSender = null;
    } else {
      this.institutionEventsSender = internalEventsConnectionString
        .filter(cs -> !cs.isBlank())
        .map(cs -> new AzureServiceBusEventSender(cs, "institution-events"))
        .orElseGet(() -> new AzureServiceBusEventSender(internalEventsNamespace.orElse(""), internalEventsClientId.orElse(""), "institution-events"));
      }
  }

  public void sendInstitutionUpdatedEvent(InstitutionEntity updatedInstitution) {
    if (institutionEventsSender == null) {
      log.debug("Internal events are disabled, skipping InstitutionUpdatedEvent publishing (institutionId: {})", updatedInstitution.getId());
      return;
    }

    if (institutionEventsSender.publish(internalEventsMapper.map(updatedInstitution))) {
      log.info("Published InstitutionUpdatedEvent successfully (institutionId: {})", updatedInstitution.getId());
    } else {
      log.error("Failed to publish InstitutionUpdatedEvent (institutionId: {})", updatedInstitution.getId());
    }
  }

  @PreDestroy
  public void destroy() {
    if (institutionEventsSender != null) {
      institutionEventsSender.close();
    }
  }

}
