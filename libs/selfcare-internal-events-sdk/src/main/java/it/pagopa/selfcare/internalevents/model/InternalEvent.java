package it.pagopa.selfcare.internalevents.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
public abstract class InternalEvent {

  public enum Status { COMPLETE, ABANDON, DEAD_LETTER }

  private final String eventId = UUID.randomUUID().toString();
  private final String eventType = this.getClass().getSimpleName();
  private final Instant eventTime = Instant.now();

  private String eventTenant;
  private String eventSource;

  @JsonIgnore
  private Status eventTargetStatus = Status.COMPLETE;

  @JsonIgnore
  private String eventDeadLetterReason;

  @JsonIgnore
  private String eventDeadLetterDescription;

}
