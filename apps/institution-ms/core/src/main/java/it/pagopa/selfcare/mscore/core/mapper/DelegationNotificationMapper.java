package it.pagopa.selfcare.mscore.core.mapper;

import it.pagopa.selfcare.mscore.constant.EventType;
import it.pagopa.selfcare.mscore.model.DelegationNotificationToSend;
import it.pagopa.selfcare.mscore.model.delegation.Delegation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = UUID.class)
public interface DelegationNotificationMapper {

    @Mapping(target = "eventType", expression = "java(toEventType(delegation.getUpdatedAt(), delegation.getClosedAt()))")
    @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "delegationId", source = "id")
    @Mapping(target = "fromType", source = "institutionType")
    @Mapping(target = "toType", source = "brokerType")
    DelegationNotificationToSend toDelegationNotificationToSend(Delegation delegation);

    default String toOffsetDateTimeString(OffsetDateTime dateTime) {
        //final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 9, 9, true)
                .appendPattern("XXX")
                .toFormatter();
        return dateTime != null ? dateTime.format(dateTimeFormatter) : null;
    }

    default EventType toEventType(OffsetDateTime updatedAt, OffsetDateTime closedAt) {
        return updatedAt != null || closedAt != null ? EventType.UPDATE : EventType.ADD;
    }

}
