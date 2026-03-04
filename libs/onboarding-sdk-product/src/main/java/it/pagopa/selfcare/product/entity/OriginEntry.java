package it.pagopa.selfcare.product.entity;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;

public class OriginEntry {

    private InstitutionType institutionType;
    private Origin origin;
    private String labelKey;

    public OriginEntry(InstitutionType institutionType, Origin origin, String labelKey) {
        this.institutionType = institutionType;
        this.origin = origin;
        this.labelKey = labelKey;
    }

    public OriginEntry() {
    }

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }
}
