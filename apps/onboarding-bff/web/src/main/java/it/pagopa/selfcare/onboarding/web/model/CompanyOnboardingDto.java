package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CompanyOnboardingDto {

    @Schema(description = "${swagger.onboarding.institutions.model.users}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Valid
    private List<CompanyUserDto> users;

    @Schema(description = "${swagger.onboarding.institutions.model.billingData}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Valid
    private CompanyBillingDataDto billingData;

    @Schema(description = "${swagger.onboarding.institutions.model.institutionType}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private InstitutionType institutionType;

    @Schema(description = "${swagger.onboarding.product.model.id}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String productId;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String taxCode;

}
