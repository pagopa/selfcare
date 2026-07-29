package it.pagopa.selfcare.onboarding.web.model;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.DataProtectionOfficer;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.GPUData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.PaymentServiceProvider;
import lombok.Data;
import jakarta.validation.Valid;
@Data
public class InstitutionData {
    @Schema(description = "${swagger.onboarding.institutions.model.id}")
    private String id;
    @Schema(description = "${swagger.onboarding.institutions.model.institutionType}")
    private InstitutionType institutionType;
    @Schema(description = "${swagger.onboarding.institutions.model.billingData}")
    private BillingDataResponseDto billingData;
    @Schema(description = "${swagger.onboarding.institutions.model.city}")
    private String city;
    @Schema(description = "${swagger.onboarding.institutions.model.county}")
    private String county;
    @Schema(description = "${swagger.onboarding.institutions.model.country}")
    private String country;
    @Schema(description = "${swagger.onboarding.institutions.model.origin}")
    private String origin;
    @Schema(description = "${swagger.onboarding.institutions.model.originId}")
    private String originId;
    @Schema(description = "${swagger.onboarding.institutions.model.paymentServiceProvider}")
    private PaymentServiceProvider paymentServiceProvider;
    @Schema(description = "${swagger.onboarding.institutions.model.dataProtectionOfficer}")
    private DataProtectionOfficer dataProtectionOfficer;
    @Schema(description = "${swagger.onboarding.institutions.model.gpuData}")
    private GPUData gpuData;
    @Schema(description = "${swagger.onboarding.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsResource companyInformations;
    @Schema(description = "${swagger.onboarding.institutions.model.assistance}")
    @Valid
    private AssistanceContactsResource assistanceContacts;
}
