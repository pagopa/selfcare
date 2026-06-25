package it.pagopa.selfcare.dashboard.model;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.dashboard.model.institution.AdditionalInformations;
import it.pagopa.selfcare.dashboard.model.institution.DataProtectionOfficer;
import it.pagopa.selfcare.dashboard.model.institution.PaymentServiceProvider;
import lombok.Data;

@Data
public class InstitutionUpdate {

    private InstitutionType institutionType;
    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String taxCode;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private AdditionalInformations additionalInformations;

}
