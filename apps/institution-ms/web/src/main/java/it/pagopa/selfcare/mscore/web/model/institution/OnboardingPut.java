package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.Data;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;

@Data
@ToString
public class OnboardingPut {

    @NotBlank
    private String productId;
    @NotBlank
    private String vatNumber;
}
