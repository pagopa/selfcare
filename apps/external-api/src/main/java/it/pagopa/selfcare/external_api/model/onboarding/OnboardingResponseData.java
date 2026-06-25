package it.pagopa.selfcare.external_api.model.onboarding;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.external_api.model.institution.Attribute;
import it.pagopa.selfcare.external_api.model.institution.BusinessData;
import it.pagopa.selfcare.external_api.model.institution.SupportContact;
import it.pagopa.selfcare.external_api.model.product.ProductInfo;
import it.pagopa.selfcare.external_api.model.user.RelationshipState;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingResponseData {
    private String id;
    private String externalId;
    private String description;
    private String taxCode;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private RelationshipState state;
    private PartyRole role;
    private ProductInfo productInfo;
    private InstitutionType institutionType;
    private String pricingPlan;
    private Billing billing;
    private String origin;
    private String originId;
    private List<Attribute> attributes;
    private BusinessData businessData;
    private SupportContact supportContact;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private String subunitCode;
    private String subunitType;
    private String aooParentCode;
    private String rootParentId;
    private String parentDescription;
    private InstitutionLocation institutionLocation;
}
