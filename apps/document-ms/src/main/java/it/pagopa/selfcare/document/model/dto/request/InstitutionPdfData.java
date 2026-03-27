package it.pagopa.selfcare.document.model.dto.request;

import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Institution data for PDF generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionPdfData {

    private String id;
    private InstitutionType institutionType;
    private String taxCode;
    private String subunitCode;
    private InstitutionPaSubunitType subunitType;

    private Origin origin;
    private String originId;
    private String city;
    private String country;
    private String county;
    private String istatCode;
    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;

    private List<GeographicTaxonomyPdfData> geographicTaxonomies;

    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;

    private String supportEmail;
    private String supportPhone;

    @Valid
    private PaymentServiceProviderPdfData paymentServiceProvider;

    @Valid
    private DataProtectionOfficerPdfData dataProtectionOfficer;

    @Valid
    private GpuDataPdfData gpuData;

    private String parentDescription;
}
