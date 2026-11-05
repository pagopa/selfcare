package it.pagopa.selfcare.dashboard.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingContractUpdateDto {

    @Schema(description = "${swagger.dashboard.institutions.model.institutionType}")
    private InstitutionType institutionType;

    @Schema(description = "${swagger.dashboard.institutions.model.origin}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String origin;

    @Schema(description = "${swagger.dashboard.institutions.model.originId}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String originId;

    @Schema(description = "${swagger.dashboard.institutions.model.fiscalCode}")
    private String taxCode;

    @Schema(description = "${swagger.dashboard.institutions.model.subunitCode}")
    private String subunitCode;

    @Schema(description = "${swagger.dashboard.institutions.model.contractUpdateUsers}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Valid
    private List<ContractUpdateUserDto> users;

}
