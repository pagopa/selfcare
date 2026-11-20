package it.pagopa.selfcare.user.internalevents;

import it.pagopa.selfcare.internalevents.model.InstitutionUpdatedEvent;
import it.pagopa.selfcare.internalevents.model.InternalEvent;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.service.UserInstitutionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class InstitutionUpdatedEventHandler {
  @Inject TenantRegistry registry;
  @Inject TenantContext context;
  @Inject UserInstitutionService service;

  @ActivateRequestContext
  public void handle(String receiverTenant, InstitutionUpdatedEvent event) {
    String tenant = registry.normalizeTenantId(receiverTenant);
    registry.resolve(tenant);
    try {
      context.setTenantId(tenant);
      if (event.getEventTenant() != null && !event.getEventTenant().isBlank()
          && !tenant.equals(registry.normalizeTenantId(event.getEventTenant()))) {
        log.warn("Rejecting internal event with tenant inconsistent with receiver {}", tenant);
        event.setEventTargetStatus(InternalEvent.Status.DEAD_LETTER);
        event.setEventDeadLetterReason("TenantMismatch");
        event.setEventDeadLetterDescription("Event tenant differs from the trusted receiver tenant");
        return;
      }
      UpdateDescriptionDto dto = new UpdateDescriptionDto();
      dto.setInstitutionDescription(event.getDescription());
      dto.setInstitutionRootName(event.getParentDescription());
      service.updateInstitutionDescription(event.getInstitutionId(), dto)
          .await().atMost(Duration.ofSeconds(5));
      event.setEventTargetStatus(InternalEvent.Status.COMPLETE);
    } finally {
      context.clear();
    }
  }
}
