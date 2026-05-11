package it.pagopa.selfcare.external_api.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.external_api.model.user.UserDto;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class OnboardingImportDto {

    @Schema(description = "${swagger.external_api.institutions.model.users}")
    @Valid
    private List<UserDto> users;

    @Schema(description = "${swagger.external_api.institutions.model.importContract}")
    @NotNull
    @Valid
    private ImportContractDto importContract;

}
