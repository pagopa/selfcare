package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.Certification;
import it.pagopa.selfcare.onboarding.client.model.CertifiedField;

public final class CertifiedFieldMapper {

    private CertifiedFieldMapper() {
    }

    public static String toValue(CertifiedField<String> certifiedField) {
        return certifiedField != null ? certifiedField.getValue() : null;
    }

    public static <T> CertifiedField<T> map(T value) {
        CertifiedField<T> resource = null;
        if (value != null) {
            resource = new CertifiedField<>();
            resource.setValue(value);
            resource.setCertification(Certification.NONE);
        }
        return resource;
    }
}
