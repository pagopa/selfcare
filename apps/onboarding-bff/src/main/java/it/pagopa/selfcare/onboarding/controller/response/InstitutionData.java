package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.client.model.DataProtectionOfficer;
import it.pagopa.selfcare.onboarding.client.model.GPUData;
import it.pagopa.selfcare.onboarding.client.model.PaymentServiceProvider;
import lombok.Data;

import jakarta.validation.Valid;

@Data
public class InstitutionData {


    @Schema(description = "${openapi.onboarding.institutions.model.id}")
    private String id;
    @Schema(description = "${openapi.onboarding.institutions.model.institutionType}")
    private InstitutionType institutionType;
    @Schema(description = "${openapi.onboarding.institutions.model.billingData}")
    private BillingDataResponseDto billingData;
    @Schema(description = "${openapi.onboarding.institutions.model.city}")
    private String city;
    @Schema(description = "${openapi.onboarding.institutions.model.county}")
    private String county;
    @Schema(description = "${openapi.onboarding.institutions.model.country}")
    private String country;
    @Schema
    private String origin;
    @Schema
    private String originId;
    @Schema(description = "${openapi.onboarding.institutions.model.paymentServiceProvider}")
    private PaymentServiceProvider paymentServiceProvider;
    @Schema(description = "${openapi.onboarding.institutions.model.dataProtectionOfficer}")
    private DataProtectionOfficer dataProtectionOfficer;
    @Schema(description = "${openapi.onboarding.institutions.model.gpuData}")
    private GPUData gpuData;
    @Schema(description = "${openapi.onboarding.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsResource companyInformations;
    @Schema(description = "${openapi.onboarding.institutions.model.assistance}")
    @Valid
    private AssistanceContactsResource assistanceContacts;
}
