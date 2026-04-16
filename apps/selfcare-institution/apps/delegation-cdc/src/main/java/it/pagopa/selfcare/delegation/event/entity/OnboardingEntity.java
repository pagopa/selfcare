package it.pagopa.selfcare.delegation.event.entity;

import it.pagopa.selfcare.delegation.event.constant.RelationshipState;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
public class OnboardingEntity {

    private String productId;
    private RelationshipState status;
    private String createdAt;
    private Boolean isAggregator;
}
