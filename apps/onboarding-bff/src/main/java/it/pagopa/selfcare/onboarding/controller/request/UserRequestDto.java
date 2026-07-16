package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequestDto {

    @Schema(description = "${openapi.onboarding.user.model.name}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @Schema(description = "${openapi.onboarding.user.model.surname}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String surname;

    @Schema(description = "${openapi.onboarding.user.model.email}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String email;
}
