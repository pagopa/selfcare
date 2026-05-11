package it.pagopa.selfcare.external_api.model.onboarding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.external_api.model.user.UserDto;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class PdaOnboardingDto {

    @Schema(description = "${swagger.external_api.institutions.model.users}")
    @NotEmpty
    @Valid
    private List<UserDto> users;

    @Schema(description = "${swagger.external_api.institutions.model.name}")
    @JsonProperty(required = true)
    @NotBlank
    private String businessName;

    @Schema(description = "${swagger.external_api.institutions.model.taxCode}")
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @Schema(description = "${swagger.external_api.product.model.id}")
    @NotNull
    private String productId;

    @Schema(description = "${swagger.external_api.institutions.model.vatNumber}")
    private String vatNumber;

    @Schema(description = "${swagger.external_api.institutions.model.recipientCode}")
    private String recipientCode;
}
