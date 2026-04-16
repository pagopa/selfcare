package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class BillingDataDto {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.name}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String businessName;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.address}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String registeredOffice;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.digitalAddress}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String digitalAddress;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.zipCode}")
    private String zipCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.taxCodeInvoicing}")
    private String taxCodeInvoicing;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.vatNumber}")
    private String vatNumber;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.recipientCode}")
    private String recipientCode;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.publicServices}")
    private Boolean publicServices;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.certified}")
    private boolean certified;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.legalForm}")
    private String legalForm;
}
