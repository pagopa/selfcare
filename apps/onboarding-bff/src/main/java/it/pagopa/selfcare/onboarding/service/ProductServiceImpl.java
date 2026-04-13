package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.client.model.product.OriginEntry;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductCreateRequestInstitutionOriginsInner;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    private final ProductApi productApi;

    public ProductServiceImpl(@RestClient ProductApi productApi) {
        this.productApi = productApi;
    }

    @Override
    public OriginResult getOrigins(String productId) {
        log.trace("getOrigins start");
        String productIdSanitized = Encode.forJava(productId);
        ProductOriginResponse origins = productApi.getProductOriginsById(productIdSanitized).await().indefinitely();
        List<OriginEntry> entries = origins == null || origins.getOrigins() == null
            ? null
            : origins.getOrigins().stream().map(this::toOriginEntry).toList();
        OriginResult originResult = OriginResult.builder().origins(entries).build();
        log.debug("getOrigins size = {}", originResult.getOrigins().size());
        log.trace("getOrigins end");
        return originResult;
    }

    private OriginEntry toOriginEntry(ProductCreateRequestInstitutionOriginsInner source) {
        OriginEntry target = new OriginEntry();
        if (source.getInstitutionType() != null) {
            target.setInstitutionType(OriginEntry.InstitutionType.valueOf(source.getInstitutionType().value()));
        }
        if (source.getOrigin() != null) {
            target.setOrigin(Origin.valueOf(source.getOrigin().value()));
        }
        target.setLabelKey(source.getLabelKey());
        return target;
    }
}
