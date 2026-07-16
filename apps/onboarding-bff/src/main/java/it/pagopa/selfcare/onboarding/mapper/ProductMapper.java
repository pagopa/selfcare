package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.product.entity.StorageOrigin;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "cdi")
public interface ProductMapper {

    List<RequiredDocumentModel> toRequiredDocumentModelList(List<org.openapi.quarkus.product_json.model.RequiredDocumentResponse> responses);

    default StorageOrigin toStorageOrigin(String value) {
        return Objects.nonNull(value) ? StorageOrigin.valueOf(value) : null;
    }
}
