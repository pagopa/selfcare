package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class OnboardingDaoTest {

    @Mock
    private InstitutionConnector institutionConnector;

    @InjectMocks
    private OnboardingDao onboardingDao;


    @Test
    void rollbackPersistOnboarding() {

        final String institutionId = "institutionId";
        final Onboarding onboarding = new Onboarding();
        onboarding.setProductId("productId");

        onboardingDao.rollbackPersistOnboarding(institutionId, onboarding);

        verify(institutionConnector, times(1))
                .findAndRemoveOnboarding(institutionId, onboarding);
    }

}
