package it.pagopa.selfcare.user.controller.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SearchUserDto {
    @JsonProperty(required = true)
    @NotNull(message = "Fiscal code is required")
    private String fiscalCode;
}
