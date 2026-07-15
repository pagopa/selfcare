package it.pagopa.selfcare.onboarding.event;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
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
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.OnboardingApi;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
@TestProfile(NotificationTestProfile.class)
class RegistryIndexServiceDisabledTest {

    @InjectMock
    @RestClient
    private OnboardingApi onboardingApi;

    @Inject
    private RegistryIndexService registryIndexService;

    @Test
    @DisplayName("Should not call updateOnboardingIndex when watcher is disabled")
    void shouldNotCallUpdateIndexWhenWatcherIsDisabled() {
        // given
        Onboarding onboarding = buildOnboarding(OnboardingStatus.COMPLETED);

        // when
        UniAssertSubscriber<Response> subscriber = registryIndexService.updateIndex(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertCompleted().awaitItem();
        verify(onboardingApi, times(0)).updateOnboardingIndex(any());
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
