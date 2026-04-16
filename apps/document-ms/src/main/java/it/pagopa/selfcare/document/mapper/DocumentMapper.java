package it.pagopa.selfcare.document.mapper;

import it.pagopa.selfcare.document.model.dto.response.DocumentResponse;
import it.pagopa.selfcare.document.model.entity.Document;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface DocumentMapper {

    DocumentResponse toResponse(Document entity);
}
