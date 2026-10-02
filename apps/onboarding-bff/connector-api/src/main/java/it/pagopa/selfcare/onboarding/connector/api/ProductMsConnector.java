package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;

import java.util.List;

public interface ProductMsConnector {

    OriginResult getOrigins(String tenantId, String productId);

    List<RequiredDocumentModel> getRequiredDocuments(String tenantId, String productId, String institutionType, String origin);

    boolean isRequiredDocumentsEnabled(String tenantId, String productId, String institutionType, String origin);

}
