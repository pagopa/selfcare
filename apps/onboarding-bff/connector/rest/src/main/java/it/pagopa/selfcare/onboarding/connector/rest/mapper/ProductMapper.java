package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.product.entity.StorageOrigin;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductOriginResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RequiredDocumentResponse;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring", implementationName = "RestProductMapperImpl")
public interface ProductMapper {

    OriginResult toOriginResult(ProductOriginResponse productOriginResponse);

    RequiredDocumentModel toRequiredDocumentModel(RequiredDocumentResponse response);

    List<RequiredDocumentModel> toRequiredDocumentModelList(List<RequiredDocumentResponse> responses);

    default StorageOrigin toStorageOrigin(String value) {
        return Objects.nonNull(value) ? StorageOrigin.valueOf(value) : null;
    }
}
