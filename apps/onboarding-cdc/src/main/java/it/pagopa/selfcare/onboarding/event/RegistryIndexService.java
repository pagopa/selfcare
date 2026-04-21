package it.pagopa.selfcare.onboarding.event;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.event.entity.Onboarding;
import it.pagopa.selfcare.onboarding.event.mapper.OnboardingMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.OnboardingApi;

import java.time.Duration;

@Slf4j
@ApplicationScoped
public class RegistryIndexService {

    @Inject
    @RestClient
    OnboardingApi onboardingApi;

    private final OnboardingMapper onboardingMapper;
    private final Boolean watchEnabled;
    private final Integer retryMinBackOff;
    private final Integer retryMaxBackOff;
    private final Integer maxRetry;

    public RegistryIndexService(
            OnboardingMapper onboardingMapper,
            @ConfigProperty(name = "onboarding-cdc.mongodb.watch.enabled") Boolean watchEnabled,
            @ConfigProperty(name = "onboarding-cdc.retry.min-backoff") Integer retryMinBackOff,
            @ConfigProperty(name = "onboarding-cdc.retry.max-backoff") Integer retryMaxBackOff,
            @ConfigProperty(name = "onboarding-cdc.retry") Integer maxRetry) {
        this.onboardingMapper = onboardingMapper;
        this.watchEnabled = watchEnabled;
        this.retryMinBackOff = retryMinBackOff;
        this.retryMaxBackOff = retryMaxBackOff;
        this.maxRetry = maxRetry;
    }

    public Uni<Response> updateIndex(Onboarding onboarding) {
        if (!watchEnabled) {
            log.debug("Registry index update is disabled, skipping onboarding id {}", onboarding.getId());
            return Uni.createFrom().item(Response.noContent().build());
        }

        assert onboarding != null;

        log.info("Sending updateOnboardingIndex for onboarding id {} with status {}", onboarding.getId(), onboarding.getStatus());

        return onboardingApi.updateOnboardingIndex(onboardingMapper.toIndexResource(onboarding))
                .onFailure().retry()
                .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofHours(retryMaxBackOff))
                .atMost(maxRetry);
    }
}
