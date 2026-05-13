package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.national_registries.LegalVerification;
import it.pagopa.selfcare.external_api.model.national_registries.LegalVerificationResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NationalRegistryMapper {
    LegalVerificationResource toResource(LegalVerification model);
}
