package it.pagopa.selfcare.dashboard.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

@Data
public class OnboardingInfo {

    @Schema(description = "${swagger.dashboard.products.model.id}")
    private String productId;

    @Schema(description = "${swagger.dashboard.products.model.status}")
    private String status;

    @Schema(description = "${swagger.dashboard.products.model.contract.available}")
    private Boolean contractAvailable;

    @Schema(description = "${swagger.dashboard.products.model.origin}")
    private String origin;

    @Schema(description = "${swagger.dashboard.products.model.originId}")
    private String originId;

    @Schema(description = "${swagger.dashboard.products.model.institutionType}")
    private InstitutionType institutionType;



}
