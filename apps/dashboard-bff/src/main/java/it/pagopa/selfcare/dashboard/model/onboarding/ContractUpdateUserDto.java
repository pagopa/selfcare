package it.pagopa.selfcare.dashboard.model.onboarding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContractUpdateUserDto {

    @Schema(description = "${swagger.dashboard.user.model.name}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @Schema(description = "${swagger.dashboard.user.model.surname}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String surname;

    @Schema(description = "${swagger.dashboard.user.model.fiscalCode}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @Schema(description = "${swagger.dashboard.user.model.role}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotNull
    private PartyRole role;

    @Schema(description = "${swagger.dashboard.user.model.institutionalEmail}")
    private String email;

}
