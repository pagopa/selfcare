package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CertifiedFieldResource<T> {

    @Schema(description = "${openapi.model.certifiedField.certified}")
    private boolean certified;

    @Schema(description = "${openapi.model.certifiedField.value}")
    private T value;

}
