package it.pagopa.selfcare.dashboard.model.onboarding;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.dashboard.model.institution.Attribute;
import it.pagopa.selfcare.dashboard.model.institution.Billing;
import it.pagopa.selfcare.dashboard.model.institution.GeographicTaxonomy;
import it.pagopa.selfcare.dashboard.model.institution.RelationshipState;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.dashboard.model.product.PnPGProductInfo;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingPnPGData {

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
    private PnPGProductInfo productInfo;
    private InstitutionType institutionType;
    private String pricingPlan;
    private Billing billing;
    private String origin;
    private List<Attribute> attributes;
    private List<GeographicTaxonomy> geographicTaxonomies;

}
