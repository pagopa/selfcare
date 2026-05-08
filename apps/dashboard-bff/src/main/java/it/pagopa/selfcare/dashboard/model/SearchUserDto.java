package it.pagopa.selfcare.dashboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchUserDto {

    @Schema(description = "${swagger.dashboard.user.model.fiscalCode}")
    @JsonProperty(required = true)
    @NotBlank(message = "Fiscal code is required")
    private String fiscalCode;

}
