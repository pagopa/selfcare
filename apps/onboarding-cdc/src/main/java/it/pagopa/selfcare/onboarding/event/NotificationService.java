package it.pagopa.selfcare.onboarding.event;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.event.entity.Onboarding;
import it.pagopa.selfcare.onboarding.event.entity.util.QueueEvent;
import it.pagopa.selfcare.onboarding.event.mapper.OnboardingMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.onboarding_functions_json.api.NotificationsApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
public class NotificationService {
    @Inject
    @RestClient
    NotificationsApi notificationsApi;
    private final OnboardingMapper onboardingMapper;
    private final Boolean watchEnabled;
    private final Integer retryMinBackOff;
    private final Integer retryMaxBackOff;
    private final Integer maxRetry;
    private final Integer minutesThresholdForUpdateNotification;

    public NotificationService(OnboardingMapper onboardingMapper,
                               @ConfigProperty(name = "onboarding-cdc.mongodb.watch.enabled") Boolean watchEnabled,
                               @ConfigProperty(name = "onboarding-cdc.retry.min-backoff") Integer retryMinBackOff,
                               @ConfigProperty(name = "onboarding-cdc.retry.max-backoff") Integer retryMaxBackOff,
                               @ConfigProperty(name = "onboarding-cdc.retry") Integer maxRetry,
                               @ConfigProperty(name = "onboarding-cdc.minutes-threshold-for-update-notification") Integer minutesThresholdForUpdateNotification) {
        this.onboardingMapper = onboardingMapper;
        this.watchEnabled = watchEnabled;
        this.retryMinBackOff = retryMinBackOff;
        this.retryMaxBackOff = retryMaxBackOff;
        this.maxRetry = maxRetry;
        this.minutesThresholdForUpdateNotification = minutesThresholdForUpdateNotification;
    }

    public Uni<OrchestrationResponse> invokeNotificationApi(Onboarding onboarding) {
        // If watch is disabled, return an empty response
        if(!watchEnabled) {
            return Uni.createFrom().item(new OrchestrationResponse());
        }

        assert onboarding != null;
        QueueEvent queueEvent = determineEventType(onboarding);
        return notificationsApi.apiNotificationPost(queueEvent.name(), onboardingMapper.toEntity(onboarding))
                .onFailure().retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofHours(retryMaxBackOff)).atMost(maxRetry);
    }

    private QueueEvent determineEventType(Onboarding onboarding) {
        return switch (onboarding.getStatus()) {
            case COMPLETED -> (isOverUpdateThreshold(onboarding.getUpdatedAt(), onboarding.getActivatedAt())) ? QueueEvent.UPDATE : QueueEvent.ADD;
            case DELETED -> QueueEvent.UPDATE;
            default -> throw new IllegalArgumentException("Onboarding status not supported");
        };
    }

    private boolean isOverUpdateThreshold(LocalDateTime updatedAt, LocalDateTime activatedAt) {
        return Objects.nonNull(updatedAt)
                && Objects.nonNull(activatedAt)
                && updatedAt.isAfter(activatedAt.plusMinutes(minutesThresholdForUpdateNotification));
    }
}
