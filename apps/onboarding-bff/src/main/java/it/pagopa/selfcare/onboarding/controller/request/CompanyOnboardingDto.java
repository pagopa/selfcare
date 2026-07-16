package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CompanyOnboardingDto {

    @Schema(description = "${openapi.onboarding.institutions.model.users}", required = true)
    @NotEmpty
    private List<@Valid CompanyUserDto> users;

    @Schema(description = "${openapi.onboarding.institutions.model.billingData}", required = true)
    @NotNull
    @Valid
    private CompanyBillingDataDto billingData;

    @Schema(description = "${openapi.onboarding.institutions.model.institutionType}", required = true)
    @NotNull
    private InstitutionType institutionType;

    @Schema(description = "${openapi.onboarding.product.model.id}", required = true)
    @NotNull
    private String productId;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}", required = true)
    @NotNull
    private String taxCode;

}
