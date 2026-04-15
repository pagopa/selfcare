package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CompanyOnboardingUserDto {
    @Schema(description = "${openapi.onboarding.institutions.model.users}", required = true)
    @NotEmpty
    @Valid
    private List<CompanyUserDto> users;

    @Schema(description = "${openapi.onboarding.institutions.model.institutionType}", required = true)
    @NotNull
    private InstitutionType institutionType = InstitutionType.PG;

    @Schema(description = "${openapi.onboarding.product.model.id}", required = true)
    @NotNull
    private String productId;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}", required = true)
    @NotNull
    private String taxCode;

    @Schema(description = "${openapi.onboarding.institutions.model.certified}", required = true)
    @JsonProperty(required = true)
    @NotNull
    private boolean certified;
}
