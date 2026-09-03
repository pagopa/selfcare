package it.pagopa.selfcare.institution.event.internalevents;

import it.pagopa.selfcare.institution.event.entity.InstitutionEntity;
import it.pagopa.selfcare.internalevents.model.InstitutionUpdatedEvent;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta", builder = @Builder(disableBuilder = true))
public interface InternalEventsMapper {

  @Mapping(target = "eventTenant", constant = "AR")
  @Mapping(target = "eventSource", constant = "institution-cdc")
  @Mapping(target = "institutionId", source = "id")
  InstitutionUpdatedEvent map(InstitutionEntity institutionEntity);

}
