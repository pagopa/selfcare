package it.pagopa.selfcare.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserDto {

    @Schema(description = "${swagger.dashboard.user.model.name}")
    private String name;

    @Schema(description = "${swagger.dashboard.user.model.surname}")
    private String surname;

    @Schema(description = "${swagger.dashboard.user.model.institutionalEmail}")
    @Email
    private String email;

    @Schema(description = "${swagger.dashboard.user.model.institutionalPhone}")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Il numero di telefono non è valido")
    private String mobilePhone;
}
