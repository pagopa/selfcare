package it.pagopa.selfcare.onboarding.connector.model.institutions;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.*;
import lombok.Data;

import java.util.Objects;

@Data
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
    private String category;
    private Billing billing;
    private InstitutionLocation institutionLocation;
    private String origin;
    private String originId;
    private InstitutionType institutionType;
    private PaymentServiceProvider paymentServiceProvider;
    private DataProtectionOfficer dataProtectionOfficer;
    private GPUData gpuData;
    private PartyRole userRole;
    private String subunitCode;
    private String subunitType;
    private String aooParentCode;

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
