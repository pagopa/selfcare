package it.pagopa.selfcare.dashboard.model.onboarding;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.dashboard.model.institution.*;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.dashboard.model.product.ProductInfo;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingData {

    private String id;
    private String externalId;
    private String originId;
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
    private List<Attribute> attributes;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private SupportContact supportContact;
    private PaymentServiceProvider paymentServiceProvider;

    private String subunitCode;
    private String subunitType;
    private String aooParentCode;
    private String rootParentId;
    private String parentDescription;

}
