package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.DataProtectionOfficer;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.GPUData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.PaymentServiceProvider;
import lombok.Data;

import jakarta.validation.Valid;

@Data
public class InstitutionData {


    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.id}")
    private String id;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.institutionType}")
    private InstitutionType institutionType;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.billingData}")
    private BillingDataResponseDto billingData;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.city}")
    private String city;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.county}")
    private String county;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.country}")
    private String country;
    @ApiModelProperty
    private String origin;
    @ApiModelProperty
    private String originId;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.paymentServiceProvider}")
    private PaymentServiceProvider paymentServiceProvider;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.dataProtectionOfficer}")
    private DataProtectionOfficer dataProtectionOfficer;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.gpuData}")
    private GPUData gpuData;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsResource companyInformations;
    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance}")
    @Valid
    private AssistanceContactsResource assistanceContacts;
}
