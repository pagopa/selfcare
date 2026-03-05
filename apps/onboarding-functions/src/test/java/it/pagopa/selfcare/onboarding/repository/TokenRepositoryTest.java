package it.pagopa.selfcare.onboarding.repository;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class TokenRepositoryTest {
    @InjectMock
    TokenRepository tokenRepository;

    @Test
    void findByOnboardingId() {
        final String onboardingId = "onboardingId";
        PanacheQuery<Token> panacheQuery = mock(PanacheQuery.class);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.of(new Token()));
        Mockito.when(tokenRepository.find(TokenRepository.ONBOARDING_ID_FIELD, onboardingId))
                .thenReturn(panacheQuery);

        // Instruct mockito to call real method that you are testing
        Mockito.when(tokenRepository.findByOnboardingId(onboardingId)).thenCallRealMethod();
        Optional<Token> token = tokenRepository.findByOnboardingId(onboardingId);
        assertTrue(token.isPresent());
    }
}
