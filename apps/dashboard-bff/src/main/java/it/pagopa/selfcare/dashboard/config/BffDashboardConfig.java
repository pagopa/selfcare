package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class BffDashboardConfig {

    private final DashboardConfig config;

    @Bean
    public ProductService productService(){
        return Optional.ofNullable(config.getBlobStorage().getConnectionStringProduct())
            .filter(cs -> !cs.isBlank())
            .map(cs -> new ProductServiceCacheable(cs, config.getBlobStorage().getContainerProduct(), config.getBlobStorage().getFilepathProduct()))
            .orElseGet(() -> new ProductServiceCacheable(config.getBlobStorage().getContainerProduct(), config.getBlobStorage().getFilepathProduct(),
                config.getBlobStorage().getAccountNameProduct(), config.getBlobStorage().getManagedIdentityClientIdProduct()));
    }

}
