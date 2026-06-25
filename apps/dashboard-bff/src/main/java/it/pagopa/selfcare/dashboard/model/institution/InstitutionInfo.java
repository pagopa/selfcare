package it.pagopa.selfcare.dashboard.model.institution;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public class InstitutionInfo {

    private String id;
    private String origin;
    private String originId;
    private InstitutionType institutionType;
    private String externalId;
    private String description;
    private String address;
    private String zipCode;
    private String city;
    private String country;
    private String county;
    private String taxCode;
    private String digitalAddress;
    private RelationshipState status;
    private String category;
    private Billing billing;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private List<GeographicTaxonomy> geographicTaxonomies;
    private SupportContact supportContact;
    private String subunitCode;
    private String subunitType;
    private String aooParentCode;
    private String parentDescription;
    private String rootParentId;
    private AdditionalInformations additionalInformations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstitutionInfo that = (InstitutionInfo) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
