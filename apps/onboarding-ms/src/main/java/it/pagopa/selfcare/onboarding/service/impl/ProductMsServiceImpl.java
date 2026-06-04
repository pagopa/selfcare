package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.ProductMsService;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

@ApplicationScoped
public class ProductMsServiceImpl implements ProductMsService {

    private final ProductApi productController;

    public ProductMsServiceImpl(@RestClient ProductApi productController) {
        this.productController = productController;
    }

    @Override
    public Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin, String productId) {
        return productController.getWorkflowType(institutionType, origin, productId);
    }
}
