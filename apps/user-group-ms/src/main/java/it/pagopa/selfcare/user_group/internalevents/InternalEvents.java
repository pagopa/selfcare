package it.pagopa.selfcare.user_group.internalevents;

import it.pagopa.selfcare.internalevents.model.InstitutionUpdatedEvent;
import it.pagopa.selfcare.internalevents.service.InternalEventReceiver;
import it.pagopa.selfcare.internalevents.service.impl.AzureServiceBusEventReceiver;
import it.pagopa.selfcare.user_group.dao.UserGroupRepository;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class InternalEvents {

  private static final String INSTITUTION_EVENTS_TOPIC = "institution-events";
  private static final String SUBSCRIPTION_NAME = "user-group-sub";

  private static final int MAX_CONCURRENT_SESSIONS = 10;
  private static final int MAX_CONCURRENT_CALLS = 1;
  private static final int GROUP_UPDATE_BATCH_SIZE = 100;

  private final UserGroupRepository userGroupRepository;
  private final InternalEventReceiver institutionEventsReceiver;

  public InternalEvents(@Value("${user-group-ms.internalevents.enabled:false}") boolean internalEventsEnabled,
                        @Value("${user-group-ms.internalevents.connection-string:}") String internalEventsConnectionString,
                        @Value("${user-group-ms.internalevents.namespace:}") String internalEventsNamespace,
                        @Value("${user-group-ms.internalevents.client-id:}") String internalEventsClientId,
                        UserGroupRepository userGroupRepository) {
    this.userGroupRepository = userGroupRepository;
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
    final String name = "Ente Aggregatore " + event.getDescription();
    final String description = "Questo gruppo contiene gli utenti dell'Ente Aggregatore '" + event.getDescription() + "'";
    final UserGroupEntity probe = new UserGroupEntity();
    probe.setParentInstitutionId(event.getInstitutionId());
    final Example<UserGroupEntity> example = Example.of(probe);
    Pageable pageable = PageRequest.of(0, GROUP_UPDATE_BATCH_SIZE, Sort.by(UserGroupEntity.Fields.ID));
    Page<UserGroupEntity> groups;

    do {
      groups = userGroupRepository.findAll(example, pageable);
      groups.forEach(group -> {
        log.info("Updating group {} with new name and description", group.getId());
        group.setName(name);
        group.setDescription(description);
      });
      userGroupRepository.saveAll(groups.getContent());
      pageable = groups.nextPageable();
    } while (groups.hasNext());
  }

}
