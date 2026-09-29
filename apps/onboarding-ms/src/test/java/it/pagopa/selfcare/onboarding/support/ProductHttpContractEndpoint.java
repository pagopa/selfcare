package it.pagopa.selfcare.onboarding.support;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;

/** Test-only bridge exercising the real request context and generated REST client. */
@Path("/product-http-contract")
@ApplicationScoped
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class ProductHttpContractEndpoint {
    @Inject ProductService productService;

    @GET
    @Path("/{operation}/{productId}")
    public Uni<Response> call(@PathParam("operation") String operation, @PathParam("productId") String productId,
                              @QueryParam("institutionType") InstitutionType institutionType,
                              @QueryParam("origin") Origin origin, @QueryParam("explicitTenant") String explicitTenant) {
        return Uni.createFrom().deferred(() -> {
            Uni<?> result = switch (operation) {
                case "product" -> productService.getProduct(productId);
                case "valid" -> Objects.isNull(explicitTenant) ? productService.getValidProduct(productId)
                        : productService.getValidProduct(productId, explicitTenant);
                case "workflow" -> productService.getWorkflowType(institutionType, origin, ProductId.fromValue(productId));
                case "documents" -> productService.getRequiredDocuments(ProductId.fromValue(productId), institutionType, origin);
                case "enabled" -> productService.isRequiredDocuments(ProductId.fromValue(productId), institutionType, origin);
                case "expiration" -> productService.getProductExpirationDays(productId);
                default -> throw new IllegalArgumentException("Unknown contract-test operation");
            };
            return result.map(value -> Response.ok(value).build());
        }).onFailure().recoverWithItem(failure -> Response.status(failure instanceof ResourceNotFoundException ? 404
                        : failure instanceof WebApplicationException http ? http.getResponse().getStatus() : 502)
                .entity(Map.of("exception", failure.getClass().getName(),
                        "detail", String.valueOf(failure.getMessage()))).build());
    }
}
