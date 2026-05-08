package it.pagopa.selfcare.dashboard.model.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserDto {

    @Schema(description = "${swagger.dashboard.user.model.name}")
    @JsonProperty(required = true)
    @NotBlank
    private String name;
    @Schema(description = "${swagger.dashboard.user.model.surname}")
    @JsonProperty(required = true)
    @NotBlank
    private String surname;
    @Schema(description = "${swagger.dashboard.user.model.institutionalEmail}")
    @JsonProperty(required = true)
    @NotBlank
    @Email
    private String email;
    @Schema(description = "${swagger.dashboard.user.model.fiscalCode}")
    @JsonProperty(required = true)
    @NotBlank
    private String fiscalCode;

}
