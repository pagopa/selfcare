package it.pagopa.selfcare.dashboard.model.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BrokerResource {

    @Schema(description = "${swagger.dashboard.brokers.model.code}")
    private String code;

    @Schema(description = "${swagger.dashboard.brokers.model.description}")
    private String description;

    @Schema(description = "${swagger.dashboard.brokers.model.enabled}")
    private Boolean enabled;

}
