package it.pagopa.selfcare.onboarding.web.model.mapper;

import it.pagopa.selfcare.onboarding.connector.model.user.Certification;
import it.pagopa.selfcare.onboarding.connector.model.user.CertifiedField;
import it.pagopa.selfcare.onboarding.web.model.CertifiedFieldResource;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CertifiedFieldMapper {

    @Named("toResource")
    default <T> CertifiedFieldResource<T> toResource(CertifiedField<T> certifiedField) {
        if (certifiedField == null) {
            return null;
        }
        CertifiedFieldResource<T> resource = new CertifiedFieldResource<>();
        resource.setCertified(Certification.isCertified(certifiedField.getCertification()));
        resource.setValue(certifiedField.getValue());
        return resource;
    }
}