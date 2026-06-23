package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import jakarta.ws.rs.core.Response;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

import java.util.List;

public interface ProductMsService {
  Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin, ProductId productId);

  Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin);

  Uni<Response> isRequiredDocumentsEnabled(ProductId productId, InstitutionType institutionType, Origin origin);
}

