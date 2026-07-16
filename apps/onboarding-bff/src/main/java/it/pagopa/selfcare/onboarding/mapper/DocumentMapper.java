package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.AvailableDocuments;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface DocumentMapper {
    AvailableDocuments toAvailableDocuments(org.openapi.quarkus.document_json.model.AvailableDocumentsResponse response);
}
