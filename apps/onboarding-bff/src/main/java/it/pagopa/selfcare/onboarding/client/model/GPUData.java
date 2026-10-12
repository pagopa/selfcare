package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "${openapi.onboarding.institutions.model.gpuData}")
public class GPUData extends BusinessData {

    private boolean manager;
    private boolean managerAuthorized;
    private boolean managerEligible;
    private boolean managerProsecution;
    private boolean institutionCourtMeasures;


}
