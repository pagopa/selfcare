package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.product.OriginResult;

public interface ProductService {

    OriginResult getOrigins(String productId);

}
