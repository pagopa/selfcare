package it.pagopa.selfcare.party.registry_proxy.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.Data;

@Data
public class StationResource {

    @ApiModelProperty(value = "${swagger.model.station.id}")
    private String id;

    @ApiModelProperty(value = "${swagger.model.station.originId}")
    private String originId;

    @ApiModelProperty(value = "${swagger.model.station.taxCode}")
    private String taxCode;

    @ApiModelProperty(value = "${swagger.model.station.description}")
    private String description;

    @ApiModelProperty(value = "${swagger.model.station.digitalAddress}")
    private String digitalAddress;

    @ApiModelProperty(value = "${swagger.model.station.anacEngaged}")
    private boolean anacEngaged;

    @ApiModelProperty(value = "${swagger.model.station.anacEnabled}")
    private boolean anacEnabled;

    @ApiModelProperty(value = "${swagger.model.*.origin}")
    private Origin origin;

}
