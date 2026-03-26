package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.rest.client.OnboardingFunctionsApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingFunctionsConnectorImplTest {

    @InjectMocks
    private OnboardingFunctionsConnectorImpl onboardingFunctionsConnector;

    @Mock
    private OnboardingFunctionsApiClient restClientMock;

    @Test
    void checkOrganization(){
        //given
        final String fiscalCode = "fiscalCode";
        final String vatNumber = "vatNumber";

        //when
        Executable executable = () -> onboardingFunctionsConnector.checkOrganization(fiscalCode, vatNumber);
        //then
        assertDoesNotThrow(executable);
        verify(restClientMock, times(1))._checkOrganization(fiscalCode, vatNumber);
        verifyNoMoreInteractions(restClientMock);
    }
}