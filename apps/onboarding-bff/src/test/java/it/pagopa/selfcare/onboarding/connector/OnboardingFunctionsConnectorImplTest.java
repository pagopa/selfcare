package it.pagopa.selfcare.onboarding.connector;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import org.openapi.quarkus.onboarding_functions_json.api.OrganizationApi;

@ExtendWith(MockitoExtension.class)
class OnboardingFunctionsConnectorImplTest {

    @InjectMocks
    private OnboardingFunctionsConnectorImpl onboardingFunctionsConnector;

    @Mock
    private OrganizationApi restClientMock;

    @Test
    void checkOrganization(){
        //given
        final String fiscalCode = "fiscalCode";
        final String vatNumber = "vatNumber";
        when(restClientMock.checkOrganization(fiscalCode, vatNumber))
            .thenReturn(Uni.createFrom().item(Response.noContent().build()));

        //when
        Executable executable = () -> onboardingFunctionsConnector.checkOrganization(fiscalCode, vatNumber);
        //then
        assertDoesNotThrow(executable);
        verify(restClientMock, times(1)).checkOrganization(fiscalCode, vatNumber);
        verifyNoMoreInteractions(restClientMock);
    }
}
