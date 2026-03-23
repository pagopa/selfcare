package it.pagopa.selfcare.party.registry_proxy.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.Data;

@Data
public class InsuranceCompanyResource implements InsuranceCompany {

    @ApiModelProperty(value = "${swagger.model.insurance-company.id}")
    private String id;

    @ApiModelProperty(value = "${swagger.model.insurance-company.originId}")
    private String originId;

    @ApiModelProperty(value = "${swagger.model.insurance-company.taxCode}")
    private String taxCode;

    @ApiModelProperty(value = "${swagger.model.insurance-company.description}")
    private String description;

    @ApiModelProperty(value = "${swagger.model.insurance-company.digitalAddress}")
    private String digitalAddress;

    @ApiModelProperty(value = "${swagger.model.insurance-company.workType}")
    private String workType;

    @ApiModelProperty(value = "${swagger.model.insurance-company.registerType}")
    private String registerType;

    @ApiModelProperty(value = "${swagger.model.insurance-company.address}")
    private String address;

    @ApiModelProperty(value = "${swagger.model.*.origin}")
    private Origin origin;

}
