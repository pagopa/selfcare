package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;

public interface ProductMsConnector {

    OriginResult getOrigins(String productId);

}
