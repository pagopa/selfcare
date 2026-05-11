package it.pagopa.selfcare.external_api.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.external_api.model.user.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingAggregatorImportDto {

  @Schema(description = "${swagger.external_api.institutions.model.users}")
  @Valid
  private List<UserDto> users;

  @Schema(description = "${swagger.external_api.institutions.model.importContract}")
  @NotNull
  @Valid
  private ImportContractDto importContract;

  @Schema(description = "${swagger.external_api.institutions.model.aggregates}")
  @NotEmpty
  @Valid
  private List<TaxCodeDto> aggregates;
}
