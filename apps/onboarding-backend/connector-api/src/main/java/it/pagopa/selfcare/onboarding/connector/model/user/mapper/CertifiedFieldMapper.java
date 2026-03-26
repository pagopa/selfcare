package it.pagopa.selfcare.onboarding.connector.model.user.mapper;

import it.pagopa.selfcare.onboarding.connector.model.user.Certification;
import it.pagopa.selfcare.onboarding.connector.model.user.CertifiedField;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CertifiedFieldMapper {

    public static String toValue(CertifiedField<String> certifiedField) {
        return certifiedField != null ? certifiedField.getValue() : null;
    }


    public static <T> CertifiedField<T> map(T certifiedField) {
        CertifiedField<T> resource = null;
        if (certifiedField != null) {
            resource = new CertifiedField<>();
            resource.setValue(certifiedField);
            resource.setCertification(Certification.NONE);
        }
        return resource;
    }

}
