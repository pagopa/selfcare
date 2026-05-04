package it.pagopa.selfcare.mscore.connector.dao.model.mapper;


import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationInstitutionEntity;
import it.pagopa.selfcare.mscore.model.delegation.Delegation;
import it.pagopa.selfcare.mscore.model.delegation.DelegationInstitution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = UUID.class)
public interface DelegationEntityMapper {

    @Mapping(target = "id", defaultExpression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "toType", source = "brokerType")
    @Mapping(target = "fromType", source = "institutionType")
    DelegationEntity convertToDelegationEntity(Delegation delegation);

    @Mapping(target = "brokerType", source = "toType")
    @Mapping(target = "institutionType", source = "fromType")
    Delegation convertToDelegation(DelegationEntity entity);

    @Mapping(target = "id", source = "createdAt", qualifiedByName = "convertToTimestampId")
    @Mapping(target = "delegationId", source = "id")
    @Mapping(target = "delegationType", source = "type")
    @Mapping(target = "delegationProductId", source = "productId")
    DelegationInstitution convertToDelegationInstitution(DelegationInstitutionEntity entity);

    @Named("convertToTimestampId")
    default long convertToTimestampId(OffsetDateTime offsetDateTime) {
        return Optional.ofNullable(offsetDateTime).map(o -> o.toInstant().toEpochMilli()).orElse(0L);
    }

}
