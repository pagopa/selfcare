package it.pagopa.selfcare.onboarding.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class OnboardingRepositoryTest {

    @InjectMock
    OnboardingRepository onboardingRepository;

    @Test
    void findByFilters() {
        final String taxCode = "taxCode";
        final String productId = "productId";
        final String originId = "originId";
        final String origin = Origin.IPA.name();
        final String subunitCode = "code";
        PanacheQuery<Onboarding> panacheQuery = mock(PanacheQuery.class);
        Onboarding onboarding = new Onboarding();
        onboarding.setId("id");
        when(panacheQuery.stream()).thenReturn(Stream.of(onboarding));
        Mockito.when(onboardingRepository.find(any()))
                .thenReturn(panacheQuery);
        Mockito.when(panacheQuery.list())
                .thenReturn(List.of(onboarding));

        // Instruct mockito to call real method that you are testing
        Mockito.when(onboardingRepository.findByFilters(taxCode, subunitCode, origin, originId, productId)).thenCallRealMethod();
        List<Onboarding> onboardings = onboardingRepository.findByFilters(taxCode, subunitCode, origin, originId, productId);
        assertFalse(onboardings.isEmpty());
    }

    @Test
    void findByOnboardingUsers() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        PanacheQuery<Onboarding> panacheQuery = mock(PanacheQuery.class);
        Onboarding onboarding = new Onboarding();
        onboarding.setId("id");
        when(panacheQuery.stream()).thenReturn(Stream.of(onboarding));
        Mockito.when(onboardingRepository.find(any()))
                .thenReturn(panacheQuery);
        Mockito.when(panacheQuery.list())
                .thenReturn(List.of(onboarding));

        // Instruct mockito to call real method that you are testing
        Mockito.when(onboardingRepository.findByOnboardingUsers(institutionId, productId)).thenCallRealMethod();
        List<Onboarding> onboardings = onboardingRepository.findByOnboardingUsers(institutionId, productId);
        assertFalse(onboardings.isEmpty());
    }

}
