package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginEntry;
import lombok.Data;

import java.util.List;

@Data
public class OriginResponse {

    @Schema(description = "${swagger.product.model.id}")
    private List<OriginEntry> origins;

}
