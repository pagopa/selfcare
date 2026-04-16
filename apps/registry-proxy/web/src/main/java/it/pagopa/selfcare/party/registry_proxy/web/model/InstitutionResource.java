package it.pagopa.selfcare.party.registry_proxy.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.Data;

@Data
public class InstitutionResource implements Institution {

    @ApiModelProperty(value = "${swagger.model.institution.id}")
    private String id;

    @ApiModelProperty(value = "${swagger.model.institution.originId}")
    private String originId;

    @ApiModelProperty(value = "${swagger.model.institution.o}")
    private String o;

    @ApiModelProperty(value = "${swagger.model.institution.ou}")
    private String ou;

    @ApiModelProperty(value = "${swagger.model.institution.aoo}")
    private String aoo;

    @ApiModelProperty(value = "${swagger.model.institution.taxCode}")
    private String taxCode;

    @ApiModelProperty(value = "${swagger.model.institution.category}")
    private String category;

    @ApiModelProperty(value = "${swagger.model.institution.description}")
    private String description;

    @ApiModelProperty(value = "${swagger.model.institution.digitalAddress}")
    private String digitalAddress;

    @ApiModelProperty(value = "${swagger.model.institution.address}")
    private String address;

    @ApiModelProperty(value = "${swagger.model.institution.zipCode}")
    private String zipCode;

    @ApiModelProperty(value = "${swagger.model.*.origin}")
    private Origin origin;

    @ApiModelProperty(value = "${swagger.model.institution.istatCode}")
    private String istatCode;

}
