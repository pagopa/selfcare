package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.document.generated.openapi.v1.dto.AvailableDocumentResponse;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.AvailableDocumentsResponse;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.AvailableDocument;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.AvailableDocuments;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    AvailableDocuments toAvailableDocuments(AvailableDocumentsResponse response);

    AvailableDocument toAvailableDocument(AvailableDocumentResponse response);
}

