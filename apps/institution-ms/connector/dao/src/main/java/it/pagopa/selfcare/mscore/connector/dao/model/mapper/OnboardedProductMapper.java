package it.pagopa.selfcare.mscore.connector.dao.model.mapper;

import it.pagopa.selfcare.mscore.connector.dao.model.inner.OnboardedProductEntity;
import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OnboardedProductMapper {
    OnboardedProduct toOnboardedProduct(OnboardedProductEntity onboardedProductEntity);
}
