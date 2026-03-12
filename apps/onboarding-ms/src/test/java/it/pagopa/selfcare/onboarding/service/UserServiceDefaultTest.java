package it.pagopa.selfcare.onboarding.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.impl.UserServiceDefault;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;

@QuarkusTest
class UserServiceDefaultTest {

    @Inject
    UserServiceDefault userServiceDefault;

    @InjectMock
    @RestClient
    InstitutionApi userInstitutionApi;

    @Test
    void retrieveUserInstitutionsTest() {
        // Given
        String institutionId = "institutionId";
        List<String> productRoles = List.of();
        List<String> products = List.of();
        List<String> roles = List.of();
        List<String> states = List.of();
        String userId = "userId";

        Uni<List<UserInstitutionResponse>> expected = Uni.createFrom().item(List.of());

        when(userInstitutionApi.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(expected);

        // When
        Uni<List<UserInstitutionResponse>> result =
                userServiceDefault.retrieveUserInstitutions(institutionId, productRoles, products, roles, states, userId);

        // Then
        verify(userInstitutionApi)
                .retrieveUserInstitutions(institutionId, productRoles, products, roles, states, userId);
        verifyNoMoreInteractions(userInstitutionApi);
    }
}
