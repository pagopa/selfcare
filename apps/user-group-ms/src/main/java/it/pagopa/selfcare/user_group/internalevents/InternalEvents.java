package it.pagopa.selfcare.user_group.internalevents;

import it.pagopa.selfcare.internalevents.model.InstitutionUpdatedEvent;
import it.pagopa.selfcare.internalevents.service.InternalEventReceiver;
import it.pagopa.selfcare.internalevents.service.impl.AzureServiceBusEventReceiver;
import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.model.UserGroupFilter;
import it.pagopa.selfcare.user_group.service.UserGroupService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class InternalEvents {

  private static final String INSTITUTION_EVENTS_TOPIC = "institution-events";
  private static final String SUBSCRIPTION_NAME = "user-ms-sub";

  private static final int MAX_CONCURRENT_SESSIONS = 10;
  private static final int MAX_CONCURRENT_CALLS = 1;

  private final UserGroupService userGroupService;
  private final InternalEventReceiver institutionEventsReceiver;

  public InternalEvents(@Value("${user-group-ms.internalevents.enabled:false}") boolean internalEventsEnabled,
                        @Value("${user-group-ms.internalevents.connection-string:}") String internalEventsConnectionString,
                        @Value("${user-group-ms.internalevents.namespace:}") String internalEventsNamespace,
                        @Value("${user-group-ms.internalevents.client-id:}") String internalEventsClientId,
                        UserGroupService userGroupService) {
    this.userGroupService = userGroupService;
    if (!internalEventsEnabled) {
      this.institutionEventsReceiver = null;
    } else {
      this.institutionEventsReceiver = Optional.ofNullable(internalEventsConnectionString)
        .filter(cs -> !cs.isBlank())
        .map(cs -> new AzureServiceBusEventReceiver(cs, INSTITUTION_EVENTS_TOPIC, SUBSCRIPTION_NAME,
          MAX_CONCURRENT_SESSIONS, MAX_CONCURRENT_CALLS))
        .orElseGet(() -> new AzureServiceBusEventReceiver(Optional.ofNullable(internalEventsNamespace).orElse(""),
          Optional.ofNullable(internalEventsClientId).orElse(""), INSTITUTION_EVENTS_TOPIC, SUBSCRIPTION_NAME,
          MAX_CONCURRENT_SESSIONS, MAX_CONCURRENT_CALLS));
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
    final Page<UserGroupOperations> groups = userGroupService.getUserGroups(
      UserGroupFilter.builder()
        .parentInstitutionId(event.getInstitutionId())
        .build(),
      Pageable.unpaged()
    );
    groups.forEach(g -> {
      log.info("Updating group {} with new name and description", g.getId());
      g.setName(event.getDescription());
      g.setDescription("Questo gruppo contiene gli utenti dell'Ente Aggregatore '" + event.getDescription() + "'");
      userGroupService.updateGroup(g.getId(), g);
    });
  }

}
