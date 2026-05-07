package it.pagopa.selfcare.dashboard.model.delegation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DelegationRequestDto {

    @Schema(description = "${swagger.dashboard.delegation.model.from}")
    @JsonProperty(required = true)
    @NotBlank
    private String from;

    @Schema(description = "${swagger.dashboard.delegation.model.to}")
    @JsonProperty(required = true)
    @NotBlank
    private String to;

    @Schema(description = "${swagger.dashboard.delegation.model.productId}")
    @JsonProperty(required = true)
    @NotBlank
    private String productId;

    @Schema(description = "${swagger.dashboard.delegation.model.type}")
    @JsonProperty(required = true)
    @DelegationTypeSubset(anyOf = {DelegationType.AOO, DelegationType.PT})
    private DelegationType type;

    @Schema(description = "${swagger.dashboard.delegation.model.institutionFromName}")
    @JsonProperty(required = true)
    @NotBlank
    private String institutionFromName;

    @Schema(description = "${swagger.dashboard.delegation.model.institutionToName}")
    @JsonProperty(required = true)
    @NotBlank
    private String institutionToName;

}
