package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CompanyOnboardingUserDto {
    @Schema(description = "${swagger.onboarding.institutions.model.users}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Valid
    private List<CompanyUserDto> users;

    @Schema(description = "${swagger.onboarding.institutions.model.institutionType}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private InstitutionType institutionType = InstitutionType.PG;

    @Schema(description = "${swagger.onboarding.product.model.id}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String productId;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String taxCode;

    @Schema(description = "${swagger.onboarding.institutions.model.certified}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotNull
    private boolean certified;
}
