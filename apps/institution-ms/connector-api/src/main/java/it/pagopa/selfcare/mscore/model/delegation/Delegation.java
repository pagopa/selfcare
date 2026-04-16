package it.pagopa.selfcare.mscore.model.delegation;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Delegation {

    private String id;
    private String from;
    private String institutionFromName;
    private String institutionToName;
    private String institutionFromRootName;
    private DelegationType type;
    private String to;
    private String productId;
    private InstitutionType institutionType;
    private String taxCode;
    private String toTaxCode;
    private String fromTaxCode;
    private InstitutionType brokerType;
    private String brokerTaxCode;
    private String fromSubunitCode;
    private String toSubunitCode;
    private DelegationState status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean isTest;
    private OffsetDateTime closedAt;
}
