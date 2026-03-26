package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;

public interface ProductService {

    OriginResult getOrigins(String productId);

}
