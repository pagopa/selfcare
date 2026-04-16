package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.mscore.model.user.UserToOnboard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OnboardingDao {

    private final InstitutionConnector institutionConnector;

    public OnboardingDao(InstitutionConnector institutionConnector) {
        this.institutionConnector = institutionConnector;
    }



    public void rollbackPersistOnboarding(String institutionId, Onboarding onboarding) {
        institutionConnector.findAndRemoveOnboarding(institutionId, onboarding);
        log.debug("rollback persistOnboarding");
    }
}
