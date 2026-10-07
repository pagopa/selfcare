package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class OnboardingUserDto {

    @Schema(description = "${openapi.onboarding.institutions.model.users}", required = true)
    @NotEmpty
    @Valid
    private List<UserDto> users;

    @Schema(description = "${openapi.onboarding.institutions.model.institutionType}")
    private InstitutionType institutionType;

    @Schema(description = "${openapi.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${openapi.onboarding.institutions.model.originId}")
    private String originId;

    @Schema(description = "${openapi.onboarding.product.model.id}", required = true)
    @NotNull
    private String productId;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${openapi.onboarding.institutions.model.subunitCode}")
    private String subunitCode;

    @Schema(description = "${openapi.onboarding.institutions.model.toAddOnAggregates}")
    private Boolean toAddOnAggregates;

}
