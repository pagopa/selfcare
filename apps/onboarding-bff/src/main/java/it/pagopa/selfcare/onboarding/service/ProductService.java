package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.OriginResult;

public interface ProductService {

    OriginResult getOrigins(String productId);

}
