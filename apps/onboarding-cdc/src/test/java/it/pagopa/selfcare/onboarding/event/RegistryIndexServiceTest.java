package it.pagopa.selfcare.onboarding.event;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.event.entity.Institution;
import it.pagopa.selfcare.onboarding.event.entity.Onboarding;
import it.pagopa.selfcare.onboarding.event.profile.NotificationTestProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openapi.quarkus.party_registry_proxy_json.api.OnboardingApi;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class RegistryIndexServiceTest {

    @InjectMock
    @RestClient
    private OnboardingApi onboardingApi;

    @Inject
    private RegistryIndexService registryIndexService;

    @Test
    @DisplayName("Should call updateOnboardingIndex for COMPLETED status")
    void shouldCallUpdateIndexForCompletedStatus() {
        Onboarding onboarding = buildOnboarding(OnboardingStatus.COMPLETED);

        when(onboardingApi.updateOnboardingIndex(any())).thenReturn(Uni.createFrom().item(Response.noContent().build()));

        UniAssertSubscriber<Response> subscriber = registryIndexService.updateIndex(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().awaitItem();
        verify(onboardingApi, times(1)).updateOnboardingIndex(any());
    }

    @Test
    @DisplayName("Should call updateOnboardingIndex for PENDING status")
    void shouldCallUpdateIndexForPendingStatus() {
        Onboarding onboarding = buildOnboarding(OnboardingStatus.PENDING);

        when(onboardingApi.updateOnboardingIndex(any())).thenReturn(Uni.createFrom().item(Response.noContent().build()));

        UniAssertSubscriber<Response> subscriber = registryIndexService.updateIndex(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().awaitItem();
        verify(onboardingApi, times(1)).updateOnboardingIndex(any());
    }

    @Test
    @DisplayName("Should call updateOnboardingIndex for DELETED status")
    void shouldCallUpdateIndexForDeletedStatus() {
        Onboarding onboarding = buildOnboarding(OnboardingStatus.DELETED);

        when(onboardingApi.updateOnboardingIndex(any())).thenReturn(Uni.createFrom().item(Response.noContent().build()));

        UniAssertSubscriber<Response> subscriber = registryIndexService.updateIndex(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().awaitItem();
        verify(onboardingApi, times(1)).updateOnboardingIndex(any());
    }

    @Nested
    @TestProfile(NotificationTestProfile.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class RegistryIndexServiceDisabledTest {

        @Test
        @DisplayName("Should not call updateOnboardingIndex when watcher is disabled")
        void shouldNotCallUpdateIndexWhenWatcherIsDisabled() {
            Onboarding onboarding = buildOnboarding(OnboardingStatus.COMPLETED);

            UniAssertSubscriber<Response> subscriber = registryIndexService.updateIndex(onboarding)
                    .subscribe().withSubscriber(UniAssertSubscriber.create());

            subscriber.assertCompleted().awaitItem();
            verify(onboardingApi, times(0)).updateOnboardingIndex(any());
        }
    }

    private Onboarding buildOnboarding(OnboardingStatus status) {
        Institution institution = new Institution();
        institution.setId("inst-id-001");
        institution.setDescription("Test Institution");
        institution.setTaxCode("12345678901");
        institution.setSubunitCode("AOO001");
        institution.setInstitutionType(InstitutionType.PA);

        Onboarding onboarding = new Onboarding();
        onboarding.id = "onboarding-id-001";
        onboarding.setProductId("prod-io");
        onboarding.setStatus(status);
        onboarding.setCreatedAt(LocalDateTime.now());
        onboarding.setUpdatedAt(LocalDateTime.now());
        onboarding.setInstitution(institution);
        return onboarding;
    }
}




