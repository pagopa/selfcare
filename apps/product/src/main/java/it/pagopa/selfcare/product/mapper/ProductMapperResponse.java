package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.model.BackOfficeRole;
import it.pagopa.selfcare.product.model.OriginEntry;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.RequiredDocument;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductRoleResponse;
import it.pagopa.selfcare.product.model.dto.response.RequiredDocumentResponse;
import org.mapstruct.Mapper;
import it.pagopa.selfcare.product.service.OnboardingRules;
import java.util.List;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapperResponse {

  ProductResponse toProductResponse(Product product);

  ProductBaseResponse toProductBaseResponse(Product product);

  @Mapping(target = "origins", source = "institutionOrigins", qualifiedByName = "selfServiceOrigins")
  ProductOriginResponse toProductOriginResponse(Product product);

  /**
   * Keeps only the self-onboardable pairs, as shown by the frontend. {@code selfOnboarding} is
   * dropped so the response shape does not change.
   */
  @Named("selfServiceOrigins")
  default List<OriginEntry> selfServiceOrigins(List<OriginEntry> origins) {
    if (origins == null) {
      return null;
    }
    return origins.stream()
        .filter(OnboardingRules::isSelfOnboardingAllowed)
        .map(
            entry ->
                OriginEntry.builder()
                    .institutionType(entry.getInstitutionType())
                    .origin(entry.getOrigin())
                    .labelKey(entry.getLabelKey())
                    .build())
        .toList();
  }

  @Mapping(target = "maxDocumentsRequired", source = "maxDocumentsRequired", defaultValue = "1")
  RequiredDocumentResponse toRequiredDocumentResponse(RequiredDocument requiredDocument);

  ProductRoleResponse toProductRoleResponse(BackOfficeRole backOfficeRole);
}
