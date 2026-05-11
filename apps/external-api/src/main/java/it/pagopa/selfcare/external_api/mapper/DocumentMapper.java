package it.pagopa.selfcare.external_api.mapper;


import it.pagopa.selfcare.document.generated.openapi.v1.dto.Document;
import it.pagopa.selfcare.document.generated.openapi.v1.dto.DocumentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    Document toEntity(DocumentResponse entity);
}
