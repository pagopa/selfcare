package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

public interface ProductMsService {
    Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin, String productId);
}

