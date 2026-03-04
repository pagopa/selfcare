package it.pagopa.selfcare.onboarding.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import it.pagopa.selfcare.onboarding.entity.Token;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class TokenRepository implements PanacheMongoRepositoryBase<Token, String> {

    public static final String ONBOARDING_ID_FIELD = "onboardingId";

    public Optional<Token> findByOnboardingId(String onboardingId) {
        return find(ONBOARDING_ID_FIELD, onboardingId).firstResultOptional();
    }
}
