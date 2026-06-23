package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.service.ProductMsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

import java.util.List;

@Slf4j
@ApplicationScoped
public class ProductMsServiceImpl implements ProductMsService {

    private final ProductApi productController;

    public ProductMsServiceImpl(@RestClient ProductApi productController) {
        this.productController = productController;
    }

    @Override
    public Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin, ProductId productId) {
        log.info("Calling getWorkflowType: productId={}, institutionType={}, origin={}", productId.getValue(), institutionType, origin);
        return productController.getWorkflowType(institutionType, origin, productId.getValue());
    }

    @Override
    public Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin) {
        log.info("Calling getRequiredDocuments: productId={}, institutionType={}, origin={}", productId.getValue(), institutionType, origin);
        return productController.getRequiredDocuments(productId.getValue(), institutionType, origin);
    }

    @Override
    public Uni<Response> isRequiredDocumentsEnabled(ProductId productId, InstitutionType institutionType, Origin origin) {
        log.info("Calling isRequiredDocumentsEnabled: productId={}, institutionType={}, origin={}", productId.getValue(), institutionType, origin);
        return productController.isRequiredDocumentsEnabled(productId.getValue(), institutionType, origin);
    }
}
