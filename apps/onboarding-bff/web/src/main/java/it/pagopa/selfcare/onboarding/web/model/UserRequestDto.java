package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequestDto {

    @ApiModelProperty(value = "${swagger.onboarding.user.model.name}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.surname}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String surname;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.email}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String email;
}
