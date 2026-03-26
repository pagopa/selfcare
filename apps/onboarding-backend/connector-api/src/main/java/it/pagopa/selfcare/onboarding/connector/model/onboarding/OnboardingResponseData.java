package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.RelationshipState;
import it.pagopa.selfcare.onboarding.connector.model.institutions.Attribute;
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
}
