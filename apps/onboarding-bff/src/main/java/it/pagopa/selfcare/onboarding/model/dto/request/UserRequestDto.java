package it.pagopa.selfcare.onboarding.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
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
