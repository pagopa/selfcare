package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GPUData extends BusinessData {

    private boolean manager;
    private boolean managerAuthorized;
    private boolean managerEligible;
    private boolean managerProsecution;
    private boolean institutionCourtMeasures;


}
