package it.pagopa.selfcare.dashboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class IdentityTokenResource {

    @Schema(description = "${swagger.dashboard.token.model.token}")
    @JsonProperty(required = true)
    @NotBlank
    private String token;

}
