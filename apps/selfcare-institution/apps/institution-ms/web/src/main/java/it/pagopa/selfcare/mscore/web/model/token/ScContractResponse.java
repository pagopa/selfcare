package it.pagopa.selfcare.mscore.web.model.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.mscore.model.QueueEvent;
import it.pagopa.selfcare.mscore.web.model.institution.BillingResponse;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScContractResponse {

    private String id;
    private String internalIstitutionID;
    private String product;
    private String state;
    private String filePath;
    private String fileName;
    private String contentType;
    private String onboardingTokenId;
    private String pricingPlan;
    private InstitutionToNotifyResponse institution;
    private BillingResponse billing;
    private OffsetDateTime createdAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime updatedAt;
    private QueueEvent notificationType;

}
