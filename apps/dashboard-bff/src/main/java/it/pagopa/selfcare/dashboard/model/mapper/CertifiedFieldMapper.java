package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.user.Certification;
import it.pagopa.selfcare.dashboard.model.user.CertifiedField;
import it.pagopa.selfcare.dashboard.model.user.CertifiedFieldResource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CertifiedFieldMapper {

    static String toValue(CertifiedField<String> certifiedField) {
        return certifiedField != null ? certifiedField.getValue() : null;
    }


    static <T> CertifiedFieldResource<T> map(CertifiedField<T> certifiedField) {
        CertifiedFieldResource<T> resource = null;
        if (certifiedField != null) {
            resource = new CertifiedFieldResource<>();
            resource.setCertified(Certification.isCertified(certifiedField.getCertification()));
            resource.setValue(certifiedField.getValue());
        }
        return resource;
    }


    static <T> CertifiedField<T> map(T certifiedField) {
        CertifiedField<T> resource = null;
        if (certifiedField != null) {
            resource = new CertifiedField<>();
            resource.setValue(certifiedField);
            resource.setCertification(Certification.NONE);
        }
        return resource;
    }

}
