package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BillingDataResponseDto {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.name}")
    private String businessName;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.address}")
    private String registeredOffice;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.digitalAddress}")
    private String digitalAddress;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.zipCode}")
    private String zipCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.vatNumber}")
    private String vatNumber;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.recipientCode}")
    private String recipientCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.publicServices}")
    private Boolean publicServices;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.certified}")
    private boolean certified;

}
