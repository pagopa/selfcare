package it.pagopa.selfcare.user.internalevents;

import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.internalevents.model.InstitutionUpdatedEvent;
import it.pagopa.selfcare.internalevents.service.InternalEventReceiver;
import it.pagopa.selfcare.internalevents.service.impl.AzureServiceBusEventReceiver;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.service.UserInstitutionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Optional;

@Startup
@ApplicationScoped
@Slf4j
public class InternalEvents {

  private static final String INSTITUTION_EVENTS_TOPIC = "institution-events";
  private static final String SUBSCRIPTION_NAME = "user-ms-sub";

  private static final int MAX_CONCURRENT_SESSIONS = 10;
  private static final int MAX_CONCURRENT_CALLS = 1;

  private final UserInstitutionService userInstitutionService;
  private final InternalEventReceiver institutionEventsReceiver;

  public InternalEvents(@ConfigProperty(name = "user-ms.internalevents.enabled", defaultValue = "false") boolean internalEventsEnabled,
                        @ConfigProperty(name = "user-ms.internalevents.connection-string") Optional<String> internalEventsConnectionString,
                        @ConfigProperty(name = "user-ms.internalevents.namespace") Optional<String> internalEventsNamespace,
                        @ConfigProperty(name = "user-ms.internalevents.client-id") Optional<String> internalEventsClientId,
                        UserInstitutionService userInstitutionService) {
    this.userInstitutionService = userInstitutionService;
    if (!internalEventsEnabled) {
      this.institutionEventsReceiver = null;
    } else {
      this.institutionEventsReceiver = internalEventsConnectionString
        .filter(cs -> !cs.isBlank())
        .map(cs -> new AzureServiceBusEventReceiver(cs, INSTITUTION_EVENTS_TOPIC, SUBSCRIPTION_NAME,
          MAX_CONCURRENT_SESSIONS, MAX_CONCURRENT_CALLS))
        .orElseGet(() -> new AzureServiceBusEventReceiver(internalEventsNamespace.orElse(""), internalEventsClientId.orElse(""),
          INSTITUTION_EVENTS_TOPIC, SUBSCRIPTION_NAME, MAX_CONCURRENT_SESSIONS, MAX_CONCURRENT_CALLS));
    }
  }

  @PostConstruct
  public void init() {
    if (institutionEventsReceiver != null) {
      institutionEventsReceiver.subscribe(InstitutionUpdatedEvent.class, this::institutionUpdatedHandler);
    }
  }

  @PreDestroy
  public void destroy() {
    if (institutionEventsReceiver != null) {
      institutionEventsReceiver.close();
    }
  }

  private void institutionUpdatedHandler(InstitutionUpdatedEvent event) {
    log.info("Received InstitutionUpdatedEvent: {}", event);
    final UpdateDescriptionDto updateDescriptionDto = new UpdateDescriptionDto();
    updateDescriptionDto.setInstitutionDescription(event.getDescription());
    updateDescriptionDto.setInstitutionRootName(event.getParentDescription());
    userInstitutionService.updateInstitutionDescription(event.getInstitutionId(), updateDescriptionDto)
      .await().atMost(Duration.ofSeconds(5));
  }

}
