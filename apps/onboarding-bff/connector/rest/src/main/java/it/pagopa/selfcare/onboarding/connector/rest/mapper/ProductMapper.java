package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductOriginResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RequiredDocumentResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", implementationName = "RestProductMapperImpl")
public interface ProductMapper {

    OriginResult toOriginResult(ProductOriginResponse productOriginResponse);

    RequiredDocumentModel toRequiredDocumentModel(RequiredDocumentResponse response);

    List<RequiredDocumentModel> toRequiredDocumentModelList(List<RequiredDocumentResponse> responses);
}
