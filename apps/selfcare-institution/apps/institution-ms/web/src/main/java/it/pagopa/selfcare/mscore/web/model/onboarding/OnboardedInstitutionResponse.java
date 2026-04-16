package it.pagopa.selfcare.mscore.web.model.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.mscore.model.institution.Billing;
import it.pagopa.selfcare.mscore.web.model.institution.AttributesResponse;
import it.pagopa.selfcare.mscore.web.model.institution.DataProtectionOfficerResponse;
import it.pagopa.selfcare.mscore.web.model.institution.GeoTaxonomies;
import it.pagopa.selfcare.mscore.web.model.institution.PaymentServiceProviderResponse;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardedInstitutionResponse {

    private String id;
    private String externalId;
    private String originId;
    private String description;
    private String origin;
    private InstitutionType institutionType;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String taxCode;
    private String pricingPlan;
    private Billing billing;
    private List<GeoTaxonomies> geographicTaxonomies;
    private List<AttributesResponse> attributes;
    private String state;
    private PartyRole role;
    private ProductInfo productInfo;
    private BusinessData businessData;
    private SupportContact supportContact;
    private PaymentServiceProviderResponse paymentServiceProvider;
    private DataProtectionOfficerResponse dataProtectionOfficer;
    private String parentDescription;
    private String rootParentId;
    private String subunitCode;
    private String subunitType;
    private String aooParentCode;

}
