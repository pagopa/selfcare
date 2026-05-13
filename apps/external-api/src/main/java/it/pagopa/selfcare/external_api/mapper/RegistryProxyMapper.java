package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.national_registries.LegalVerification;
import it.pagopa.selfcare.registry_proxy.generated.openapi.v1.dto.LegalVerificationResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RegistryProxyMapper {

    LegalVerification toLegalVerification(LegalVerificationResult model);
}
