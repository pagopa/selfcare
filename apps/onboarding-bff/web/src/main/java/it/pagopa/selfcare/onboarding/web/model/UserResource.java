package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;

import java.util.UUID;

@Data
public class UserResource {

    @ApiModelProperty(value = "${swagger.onboarding.user.model.id}")
    private UUID id;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.name}")
    private String name;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.surname}")
    private String surname;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.institutionalEmail}")
    private String email;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.fiscalCode}")
    private String taxCode;

    @ApiModelProperty(value = "${swagger.onboarding.user.model.role}")
    private PartyRole role;


    @ApiModelProperty(value = "${swagger.onboarding.user.model.status}")
    private String status;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.id}")
    private UUID institutionId;

}
