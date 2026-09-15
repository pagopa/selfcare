package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.party.registry_proxy.connector.rest.utils.LogSanitizer.sanitize;

/**
 * Selects the PDND subscription credentials to use based on the calling product.
 * Products listed in {@code rest-client.pdnd-infocamere.invitalia-products} use the
 * Invitalia subscription; every other product uses the default InfoCamere subscription.
 */
@Slf4j
@Component
public class PdndSecretValueResolver {

    private final PDNDInfoCamereRestClientConfig infoCamereConfig;
    private final PDNDInvitaliaRestClientConfig invitaliaConfig;
    private final Set<String> invitaliaProducts;

    public PdndSecretValueResolver(
            PDNDInfoCamereRestClientConfig infoCamereConfig,
            PDNDInvitaliaRestClientConfig invitaliaConfig,
            @Value("${rest-client.pdnd-infocamere.invitalia-products:}") String invitaliaProducts) {
        this.infoCamereConfig = infoCamereConfig;
        this.invitaliaConfig = invitaliaConfig;
        this.invitaliaProducts = Arrays.stream(invitaliaProducts.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public PdndSecretValue resolve(String productId) {
        if (Objects.nonNull(productId) && invitaliaProducts.contains(productId)) {
            log.info("Using Invitalia PDND subscription for product {}", sanitize(productId));
            return invitaliaConfig.getPdndSecretValue();
        }
        log.info("Using InfoCamere PDND subscription");
        return infoCamereConfig.getPdndSecretValue();
    }
}

