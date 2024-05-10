package it.pagopa.selfcare.user.controller.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SearchUserDto {
    @JsonProperty(required = true)
    @NotNull
    private String fiscalCode;
}
