package it.pagopa.selfcare.party.registry_proxy.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.Data;

@Data
public class CategoryResource implements Category {

    @ApiModelProperty(value = "${swagger.model.category.code}")
    private String code;

    @ApiModelProperty(value = "${swagger.model.category.name}")
    private String name;

    @ApiModelProperty(value = "${swagger.model.category.kind}")
    private String kind;

    @ApiModelProperty(value = "${swagger.model.*.origin}")
    private Origin origin;

}
