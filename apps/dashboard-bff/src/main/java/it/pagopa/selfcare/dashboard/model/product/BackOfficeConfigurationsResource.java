package it.pagopa.selfcare.dashboard.model.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BackOfficeConfigurationsResource {

    @Schema(description = "${swagger.dashboard.product-backoffice-configurations.model.environment}")
    private String environment;

    @Schema(description = "${swagger.dashboard.products.model.urlBO}")
    private String url;

}
