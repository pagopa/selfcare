package it.pagopa.selfcare.internalevents.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@InternalEventTopic("institution-events")
public class InstitutionUpdatedEvent extends InternalEvent {

  @InternalEventSessionKey
  private String institutionId;

  private String description;
  private String parentDescription;

}
