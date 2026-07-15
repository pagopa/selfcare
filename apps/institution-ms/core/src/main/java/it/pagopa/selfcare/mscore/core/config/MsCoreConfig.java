package it.pagopa.selfcare.mscore.core.config;

import it.pagopa.selfcare.mscore.config.CoreConfig;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.product.service.ProductServiceCacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class MsCoreConfig {

    private final CoreConfig config;

    @Bean
    public ProductService productService(){
        return Optional.ofNullable(config.getBlobStorage().getConnectionStringProduct())
            .filter(cs -> !cs.isBlank())
            .map(cs -> new ProductServiceCacheable(cs, config.getBlobStorage().getContainerProduct(), config.getBlobStorage().getFilepathProduct()))
            .orElseGet(() -> new ProductServiceCacheable(config.getBlobStorage().getContainerProduct(), config.getBlobStorage().getFilepathProduct(),
                config.getBlobStorage().getAccountNameProduct(), config.getBlobStorage().getManagedIdentityClientIdProduct()));
    }

    @Bean
    public SesClient sesClient() {

        StaticCredentialsProvider staticCredentials = StaticCredentialsProvider
                .create(AwsBasicCredentials.create(config.getAwsSesSecretId(), config.getAwsSesSecretKey()));

        return SesClient.builder()
                .region(Region.of(config.getAwsSesRegion()))
                .credentialsProvider(staticCredentials)
                .build();
    }

}
