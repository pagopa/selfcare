package it.pagopa.selfcare.external_api.model.user.mapper;


import it.pagopa.selfcare.external_api.model.user.Certification;
import it.pagopa.selfcare.external_api.model.user.CertifiedField;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CertifiedFieldMapper {

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
