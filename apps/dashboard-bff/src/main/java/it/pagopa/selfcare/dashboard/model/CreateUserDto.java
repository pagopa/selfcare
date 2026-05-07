package it.pagopa.selfcare.dashboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class CreateUserDto {

    @Schema(description = "${swagger.dashboard.user.model.name}")
    private String name;

    @Schema(description = "${swagger.dashboard.user.model.surname}")
    private String surname;

    @Schema(description = "${swagger.dashboard.user.model.fiscalCode}")
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @Schema(description = "${swagger.dashboard.user.model.email}")
    @Email
    private String email;

    @Schema(description = "${swagger.dashboard.user.model.role}")
    String role;

    @Schema(description = "${swagger.dashboard.user.model.productRoles}")
    @JsonProperty(required = true)
    @NotEmpty
    private Set<String> productRoles;

    @Schema(description = "${swagger.dashboard.user.model.toAddOnAggregates}")
    private Boolean toAddOnAggregates;

}
