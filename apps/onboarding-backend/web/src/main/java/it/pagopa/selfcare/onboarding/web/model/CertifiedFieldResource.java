package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CertifiedFieldResource<T> {

    @ApiModelProperty(value = "${swagger.model.certifiedField.certified}")
    private boolean certified;

    @ApiModelProperty(value = "${swagger.model.certifiedField.value}")
    private T value;

}
