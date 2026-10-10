package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.IamRestClient;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The HTTP behavior of the IAM client (headers, timeouts, errors) is covered by the runtime tests under {@code runtime}. */
@ExtendWith(MockitoExtension.class)
class IamServiceImplTest {

    private static final String PERMISSION = "Selc:ManageAccountPage";
    private static final String USER_ID = "user-uid";
    private static final String INSTITUTION_ID = "institution-id";
    private static final String PRODUCT_ID = "prod-test";

    @InjectMocks
    private IamServiceImpl iamService;

    @Mock
    private IamRestClient iamRestClient;

    @Test
    void hasIamUserPermission_authorized_returnsTrue() {
        Response response = responseWithBody(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID)).thenReturn(response);

        assertTrue(iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID));

        verify(response).close();
    }

    @Test
    void hasIamUserPermission_emptyInstitutionQueryIsOmitted() {
        Response response = responseWithBody(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, null, PRODUCT_ID)).thenReturn(response);

        assertTrue(iamService.hasIamUserPermission(PERMISSION, USER_ID, "", PRODUCT_ID));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void hasIamUserPermission_emptyQueriesAreOmitted(String value) {
        Response response = responseWithBody(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, null, null)).thenReturn(response);

        assertTrue(iamService.hasIamUserPermission(PERMISSION, USER_ID, value, value));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "scoped-id"})
    void hasIamUserPermission_nonEmptyQueriesArePreserved(String value) {
        Response response = responseWithBody(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, value, value)).thenReturn(response);

        assertTrue(iamService.hasIamUserPermission(PERMISSION, USER_ID, value, value));
    }

    @Test
    void hasIamUserPermission_notAuthorized_returnsFalse() {
        Response response = responseWithBody(Map.of("hasPermission", false));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID)).thenReturn(response);

        assertFalse(iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID));
    }

    @Test
    void hasIamUserPermission_bodyWithoutFlag_returnsFalse() {
        Response response = responseWithBody(Map.of());
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID)).thenReturn(response);

        assertFalse(iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID));
    }

    @Test
    void hasIamUserPermission_emptyBody_returnsFalse() {
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(false);
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID)).thenReturn(response);

        assertFalse(iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID));

        verify(response).close();
    }

    @Test
    void hasIamUserPermission_iamFailure_isPropagatedNotTurnedIntoADenial() {
        WebApplicationException failure = new WebApplicationException(Response.status(403).build());
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID)).thenThrow(failure);

        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID));

        assertSame(failure, thrown);
    }

    private static Response responseWithBody(Map<String, Object> body) {
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(Map.class)).thenReturn(body);
        return response;
    }
}
