package it.pagopa.selfcare.mscore.model;

import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.constant.EventType;
import lombok.Data;

@Data
public class DelegationNotificationToSend {

    private String id;
    private String delegationId;
    private String from;
    private String institutionFromName;
    private String institutionToName;
    private String institutionFromRootName;
    private String to;
    private String toTaxCode;
    private String fromTaxCode;
    private String toType;
    private String fromType;
    private String productId;
    private DelegationType type;
    private DelegationState status;
    private String createdAt;
    private String updatedAt;
    private String closedAt;
    private Boolean isTest;
    private EventType eventType;

}
