package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CompanyOnboardingUserDto {
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.users}", required = true)
    @NotEmpty
    @Valid
    private List<CompanyUserDto> users;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.institutionType}", required = true)
    @NotNull
    private InstitutionType institutionType = InstitutionType.PG;

    @ApiModelProperty(value = "${swagger.onboarding.product.model.id}", required = true)
    @NotNull
    private String productId;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.taxCode}", required = true)
    @NotNull
    private String taxCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.certified}", required = true)
    @JsonProperty(required = true)
    @NotNull
    private boolean certified;
}
