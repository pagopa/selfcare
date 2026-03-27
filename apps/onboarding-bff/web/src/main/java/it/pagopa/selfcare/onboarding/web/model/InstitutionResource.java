package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import lombok.Data;

import java.util.UUID;

@Data
public class InstitutionResource {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.id}")
    private UUID id;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.name}")
    private String description;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.parentDescription}")
    private String parentDescription;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.externalId}")
    private String externalId;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.originId}")
    private String originId;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.institutionType}")
    private String institutionType;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.digitalAddress}")
    private String digitalAddress;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.address}")
    private String address;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.zipCode}")
    private String zipCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.city}")
    private String city;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.county}")
    private String county;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.country}")
    private String country;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.origin}")
    private String origin;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.userRole}")
    private SelfCareAuthority userRole;

}
