package it.pagopa.selfcare.dashboard.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CertifiedFieldResource<T> {

    @Schema(description = "${swagger.model.certifiedField.certified}")
    private boolean certified;

    @Schema(description = "${swagger.model.certifiedField.value}")
    private T value;

}
