package it.pagopa.selfcare.external_api.model.institution;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.external_api.model.onboarding.Billing;
import it.pagopa.selfcare.external_api.model.onboarding.DataProtectionOfficer;
import it.pagopa.selfcare.external_api.model.onboarding.InstitutionLocation;
import it.pagopa.selfcare.external_api.model.onboarding.PaymentServiceProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;


@Data
@EqualsAndHashCode
public class InstitutionInfo {

    private String id;
    private String externalId;
    private String description;
    private String status;
    private String taxCode;
    private String address;
    private String digitalAddress;
    private String pricingPlan;
    private String zipCode;
    private InstitutionLocation institutionLocation;
    private String category;
    private Billing billing;
    private String origin;
    private String originId;
    private InstitutionType institutionType;
    private PartyRole userRole;
    private Collection<String> productRoles;
    private BusinessData businessData;
    private SupportContact supportContact;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private String subunitCode;
    private String subunitType;
    private RootParent rootParent;
    private String aooParentCode;

}
