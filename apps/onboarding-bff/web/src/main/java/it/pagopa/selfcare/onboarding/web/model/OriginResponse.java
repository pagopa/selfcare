package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginEntry;
import lombok.Data;

import java.util.List;

@Data
public class OriginResponse {

    @ApiModelProperty(value = "${swagger.product.model.id}")
    private List<OriginEntry> origins;

}
