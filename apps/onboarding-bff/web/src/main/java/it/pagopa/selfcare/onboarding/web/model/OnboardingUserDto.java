package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class OnboardingUserDto {

    @Schema(description = "${swagger.onboarding.institutions.model.users}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Valid
    private List<UserDto> users;

    @Schema(description = "${swagger.onboarding.institutions.model.institutionType}")
    private InstitutionType institutionType;

    @Schema(description = "${swagger.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.onboarding.institutions.model.originId}")
    private String originId;

    @Schema(description = "${swagger.onboarding.product.model.id}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String productId;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.onboarding.institutions.model.subunitCode}")
    private String subunitCode;

    @Schema(description = "${swagger.onboarding.institutions.model.toAddOnAggregates}")
    private Boolean toAddOnAggregates;

}
