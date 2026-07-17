package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.document.generated.openapi.v1.dto.AvailableDocumentsResponse;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.AvailableDocuments;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    AvailableDocuments toAvailableDocuments(AvailableDocumentsResponse response);
}

