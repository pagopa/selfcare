package it.pagopa.selfcare.onboarding.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.onboarding.client.model.product.OriginEntry;
import lombok.Data;

import java.util.List;

@Data
public class OriginResponse {

    @ApiModelProperty(value = "${swagger.product.model.id}")
    private List<OriginEntry> origins;

}
